package com.eugeniokg.corebankingledger.account;

import com.eugeniokg.corebankingledger.security.AuthenticatedPrincipal;
import com.eugeniokg.corebankingledger.security.CurrentUserProvider;
import com.eugeniokg.corebankingledger.security.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    public AccountService(AccountRepository accountRepository, CurrentUserProvider currentUserProvider) {
        this.accountRepository = accountRepository;
        this.currentUserProvider = currentUserProvider;
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

    public Account create(Account account) {
        return accountRepository.save(account);
    }
}
