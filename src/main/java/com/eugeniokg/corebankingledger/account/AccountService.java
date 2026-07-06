package com.eugeniokg.corebankingledger.account;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account findById(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public List<Account> findByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public Account create(Account account) {
        return accountRepository.save(account);
    }
}
