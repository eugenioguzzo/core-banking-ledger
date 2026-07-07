package com.eugeniokg.corebankingledger.security;

import com.eugeniokg.corebankingledger.IntegrationTestSupport;
import com.eugeniokg.corebankingledger.account.Account;
import com.eugeniokg.corebankingledger.account.AccountRepository;
import com.eugeniokg.corebankingledger.account.AccountResponse;
import com.eugeniokg.corebankingledger.account.AccountStatus;
import com.eugeniokg.corebankingledger.account.Customer;
import com.eugeniokg.corebankingledger.account.CustomerRepository;
import com.eugeniokg.corebankingledger.common.ErrorResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTests extends IntegrationTestSupport {

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
    private JwtProperties jwtProperties;

    @Test
    void loginSucceedsAndIssuesTokens() {
        String email = uniqueEmail();
        createCustomerWithAccount(email, BigDecimal.TEN);

        ResponseEntity<TokenResponse> response = login(email, PASSWORD);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse body = response.getBody();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.tokenType()).isEqualTo("Bearer");
        assertThat(body.expiresInSeconds()).isPositive();
    }

    @Test
    void failedLoginDoesNotRevealWhetherEmailExists() {
        String existingEmail = uniqueEmail();
        createCustomerWithAccount(existingEmail, BigDecimal.TEN);

        ResponseEntity<ErrorResponse> wrongPassword = loginExpectingError(existingEmail, "wrong-password");
        ResponseEntity<ErrorResponse> unknownEmail = loginExpectingError(uniqueEmail(), "any-password");

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody().message()).isEqualTo(unknownEmail.getBody().message());
    }

    @Test
    void customerCannotAccessAnotherCustomersAccount() {
        String emailA = uniqueEmail();
        Account accountA = createCustomerWithAccount(emailA, BigDecimal.TEN);
        String emailB = uniqueEmail();
        Account accountB = createCustomerWithAccount(emailB, BigDecimal.TEN);

        String tokenA = login(emailA, PASSWORD).getBody().accessToken();

        ResponseEntity<ErrorResponse> response = getAccountExpectingError(accountB.getId(), tokenA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // A customer can still read their own account with the same token.
        ResponseEntity<AccountResponse> ownAccount = getAccount(accountA.getId(), tokenA);
        assertThat(ownAccount.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unauthenticatedRequestsAreRejectedWith401() {
        String email = uniqueEmail();
        Account account = createCustomerWithAccount(email, BigDecimal.TEN);

        ResponseEntity<ErrorResponse> withoutToken = restTemplate.exchange(
                "/accounts/" + account.getId(), HttpMethod.GET, HttpEntity.EMPTY, ErrorResponse.class);
        assertThat(withoutToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<ErrorResponse> withGarbageToken = getAccountExpectingError(account.getId(), "not-a-real-jwt");
        assertThat(withGarbageToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String expiredToken = buildExpiredAccessToken(email);
        ResponseEntity<ErrorResponse> withExpiredToken = getAccountExpectingError(account.getId(), expiredToken);
        assertThat(withExpiredToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshTokenIssuesNewValidAccessToken() {
        String email = uniqueEmail();
        Account account = createCustomerWithAccount(email, BigDecimal.TEN);
        TokenResponse initialTokens = login(email, PASSWORD).getBody();

        HttpEntity<RefreshRequest> refreshRequest = new HttpEntity<>(new RefreshRequest(initialTokens.refreshToken()));
        ResponseEntity<TokenResponse> refreshResponse =
                restTemplate.postForEntity("/auth/refresh", refreshRequest, TokenResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = refreshResponse.getBody().accessToken();
        assertThat(newAccessToken).isNotBlank();

        ResponseEntity<AccountResponse> response = getAccount(account.getId(), newAccessToken);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void operatorCannotCreateOrPromoteUserToAdmin() {
        String operatorEmail = uniqueEmail();
        createStaffUser(Role.OPERATOR, operatorEmail);
        String operatorToken = login(operatorEmail, PASSWORD).getBody().accessToken();

        CreateUserRequest createAdminRequest = new CreateUserRequest(uniqueEmail(), "another-password", Role.ADMIN, null);
        ResponseEntity<ErrorResponse> createResponse = restTemplate.exchange("/users", HttpMethod.POST,
                new HttpEntity<>(createAdminRequest, authHeaders(operatorToken)), ErrorResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        User existingCustomerUser = createStaffUser(Role.CUSTOMER, uniqueEmail());
        ChangeRoleRequest promoteRequest = new ChangeRoleRequest(Role.ADMIN);
        ResponseEntity<ErrorResponse> promoteResponse = restTemplate.exchange(
                "/users/" + existingCustomerUser.getId() + "/role", HttpMethod.PUT,
                new HttpEntity<>(promoteRequest, authHeaders(operatorToken)), ErrorResponse.class);
        assertThat(promoteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Account createCustomerWithAccount(String email, BigDecimal balance) {
        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer.setTaxId(UUID.randomUUID().toString().substring(0, 16));
        customer.setEmail(UUID.randomUUID() + "@customers.example.com");
        customer = customerRepository.save(customer);

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

    private User createStaffUser(Role role, String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private ResponseEntity<TokenResponse> login(String email, String password) {
        return restTemplate.postForEntity("/auth/login", new LoginRequest(email, password), TokenResponse.class);
    }

    private ResponseEntity<ErrorResponse> loginExpectingError(String email, String password) {
        return restTemplate.postForEntity("/auth/login", new LoginRequest(email, password), ErrorResponse.class);
    }

    private ResponseEntity<AccountResponse> getAccount(UUID accountId, String accessToken) {
        return restTemplate.exchange("/accounts/" + accountId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(accessToken)), AccountResponse.class);
    }

    private ResponseEntity<ErrorResponse> getAccountExpectingError(UUID accountId, String accessToken) {
        return restTemplate.exchange("/accounts/" + accountId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(accessToken)), ErrorResponse.class);
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String buildExpiredAccessToken(String email) {
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(3600);
        return Jwts.builder()
                .subject(email)
                .claim("role", Role.CUSTOMER.name())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(signingKey)
                .compact();
    }
}
