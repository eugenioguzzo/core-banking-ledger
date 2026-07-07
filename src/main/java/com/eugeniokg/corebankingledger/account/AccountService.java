package com.eugeniokg.corebankingledger.account;

import com.eugeniokg.corebankingledger.audit.AuditEvent;
import com.eugeniokg.corebankingledger.common.Masking;
import com.eugeniokg.corebankingledger.security.AuthenticatedPrincipal;
import com.eugeniokg.corebankingledger.security.CurrentUserProvider;
import com.eugeniokg.corebankingledger.security.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository,
                           CurrentUserProvider currentUserProvider, ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    public Account findById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    /**
     * Looks up an account and, for a CUSTOMER principal, verifies that it belongs to them.
     * This is enforced here - not only via @PreAuthorize on the controller - so a customer
     * can never reach another customer's account by guessing or otherwise obtaining its id.
     */
    public Account findAccessibleById(UUID id) {
        Account account = findById(id);
        assertOwnedByCurrentUserIfCustomer(account);
        return account;
    }

    public void assertOwnedByCurrentUserIfCustomer(Account account) {
        AuthenticatedPrincipal currentUser = currentUserProvider.getCurrentUser();
        if (currentUser.getRole() == Role.CUSTOMER
                && !account.getCustomer().getId().equals(currentUser.getCustomerId())) {
            throw new AccessDeniedException("Customers can only access or operate on their own accounts");
        }
    }

    public List<Account> findByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(request.currency());
        account.setStatus(AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        eventPublisher.publishEvent(new AuditEvent(
                currentUserProvider.getCurrentUser().getUsername(),
                "ACCOUNT_CREATED",
                "Account",
                account.getId().toString(),
                Map.of(
                        "customerId", customer.getId().toString(),
                        "accountNumber", Masking.maskAccountNumber(account.getAccountNumber()),
                        "currency", account.getCurrency())));

        return account;
    }

    @Transactional
    public Account updateStatus(UUID id, UpdateAccountStatusRequest request) {
        Account account = findById(id);
        AccountStatus previousStatus = account.getStatus();
        account.setStatus(request.status());
        account = accountRepository.save(account);

        eventPublisher.publishEvent(new AuditEvent(
                currentUserProvider.getCurrentUser().getUsername(),
                auditActionFor(request.status()),
                "Account",
                account.getId().toString(),
                Map.of("previousStatus", previousStatus.name(), "newStatus", request.status().name())));

        return account;
    }

    private String auditActionFor(AccountStatus status) {
        return switch (status) {
            case SUSPENDED -> "ACCOUNT_BLOCKED";
            case CLOSED -> "ACCOUNT_CLOSED";
            case ACTIVE -> "ACCOUNT_REACTIVATED";
        };
    }

    private String generateAccountNumber() {
        return "ACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    public Account create(Account account) {
        return accountRepository.save(account);
    }
}
