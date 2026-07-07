package com.eugeniokg.corebankingledger.audit;

import com.eugeniokg.corebankingledger.IntegrationTestSupport;
import com.eugeniokg.corebankingledger.account.Account;
import com.eugeniokg.corebankingledger.account.AccountRepository;
import com.eugeniokg.corebankingledger.account.AccountResponse;
import com.eugeniokg.corebankingledger.account.AccountStatus;
import com.eugeniokg.corebankingledger.account.CreateAccountRequest;
import com.eugeniokg.corebankingledger.account.Customer;
import com.eugeniokg.corebankingledger.account.CustomerRepository;
import com.eugeniokg.corebankingledger.account.UpdateAccountStatusRequest;
import com.eugeniokg.corebankingledger.security.LoginRequest;
import com.eugeniokg.corebankingledger.security.Role;
import com.eugeniokg.corebankingledger.security.TokenResponse;
import com.eugeniokg.corebankingledger.security.User;
import com.eugeniokg.corebankingledger.security.UserRepository;
import com.eugeniokg.corebankingledger.transaction.TransferRequest;
import com.eugeniokg.corebankingledger.transaction.TransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTrailIntegrationTests extends IntegrationTestSupport {

    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void accountCreationAndStatusChangeAreAudited() {
        String adminEmail = uniqueEmail();
        createStaffUser(Role.ADMIN, adminEmail);
        String adminToken = login(adminEmail, PASSWORD).accessToken();

        Customer customer = createCustomer();

        CreateAccountRequest createRequest = new CreateAccountRequest(customer.getId(), "EUR");
        ResponseEntity<AccountResponse> createResponse = restTemplate.exchange("/accounts", HttpMethod.POST,
                new HttpEntity<>(createRequest, authHeaders(adminToken)), AccountResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID accountId = createResponse.getBody().id();

        List<AuditLog> creationEntries = auditLogRepository.findByEntityTypeAndEntityId("Account", accountId.toString());
        assertThat(creationEntries).anySatisfy(entry -> {
            assertThat(entry.getAction()).isEqualTo("ACCOUNT_CREATED");
            assertThat(entry.getUsername()).isEqualTo(adminEmail);
        });

        UpdateAccountStatusRequest blockRequest = new UpdateAccountStatusRequest(AccountStatus.SUSPENDED);
        ResponseEntity<AccountResponse> blockResponse = restTemplate.exchange("/accounts/" + accountId + "/status",
                HttpMethod.PUT, new HttpEntity<>(blockRequest, authHeaders(adminToken)), AccountResponse.class);
        assertThat(blockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<AuditLog> statusEntries = auditLogRepository.findByEntityTypeAndEntityId("Account", accountId.toString());
        assertThat(statusEntries).anySatisfy(entry -> assertThat(entry.getAction()).isEqualTo("ACCOUNT_BLOCKED"));
    }

    @Test
    void successfulAndFailedTransfersAreAudited() {
        String email = uniqueEmail();
        Account source = createCustomerWithAccount(email, new BigDecimal("100.00"));
        Account destination = createCustomerWithAccount(uniqueEmail(), BigDecimal.ZERO);
        String token = login(email, PASSWORD).accessToken();

        HttpHeaders successHeaders = authHeaders(token);
        successHeaders.set("Idempotency-Key", "audit-success-" + UUID.randomUUID());
        TransferRequest successRequest = new TransferRequest(source.getId(), destination.getId(),
                new BigDecimal("10.00"), "audited transfer");
        ResponseEntity<TransferResponse> successResponse = restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(successRequest, successHeaders), TransferResponse.class);
        assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID transactionId = successResponse.getBody().id();

        List<AuditLog> completedEntries =
                auditLogRepository.findByEntityTypeAndEntityId("Transaction", transactionId.toString());
        assertThat(completedEntries).anySatisfy(entry -> {
            assertThat(entry.getAction()).isEqualTo("TRANSFER_COMPLETED");
            assertThat(entry.getUsername()).isEqualTo(email);
        });

        HttpHeaders failureHeaders = authHeaders(token);
        failureHeaders.set("Idempotency-Key", "audit-failure-" + UUID.randomUUID());
        TransferRequest failureRequest = new TransferRequest(source.getId(), destination.getId(),
                new BigDecimal("1000.00"), "should fail");
        ResponseEntity<String> failureResponse = restTemplate.exchange("/transactions", HttpMethod.POST,
                new HttpEntity<>(failureRequest, failureHeaders), String.class);
        assertThat(failureResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        List<AuditLog> failedEntries = auditLogRepository.findAll().stream()
                .filter(entry -> "TRANSFER_FAILED".equals(entry.getAction()) && email.equals(entry.getUsername()))
                .toList();
        assertThat(failedEntries).isNotEmpty();
    }

    @Test
    void loginAttemptsAreAudited() {
        String email = uniqueEmail();
        createCustomerWithAccount(email, BigDecimal.TEN);

        ResponseEntity<TokenResponse> success = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, PASSWORD), TokenResponse.class);
        assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);

        restTemplate.postForEntity("/auth/login", new LoginRequest(email, "wrong-password"), String.class);

        List<AuditLog> entriesForUser = auditLogRepository.findAll().stream()
                .filter(entry -> email.equals(entry.getUsername()))
                .toList();
        assertThat(entriesForUser).anySatisfy(entry -> assertThat(entry.getAction()).isEqualTo("LOGIN_SUCCESS"));
        assertThat(entriesForUser).anySatisfy(entry -> assertThat(entry.getAction()).isEqualTo("LOGIN_FAILURE"));
    }

    private Customer createCustomer() {
        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer.setTaxId(UUID.randomUUID().toString().substring(0, 16));
        customer.setEmail(UUID.randomUUID() + "@customers.example.com");
        return customerRepository.save(customer);
    }

    private Account createCustomerWithAccount(String email, BigDecimal balance) {
        Customer customer = createCustomer();

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(UUID.randomUUID().toString().substring(0, 20));
        account.setBalance(balance);
        account.setCurrency("EUR");
        account.setStatus(AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setCustomerId(customer.getId());
        user.setEnabled(true);
        userRepository.save(user);

        return account;
    }

    private void createStaffUser(Role role, String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private TokenResponse login(String email, String password) {
        return restTemplate.postForEntity("/auth/login", new LoginRequest(email, password), TokenResponse.class)
                .getBody();
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
