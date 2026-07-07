package com.eugeniokg.corebankingledger.account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(UUID id, String accountNumber, BigDecimal balance, String currency,
                               AccountStatus status, UUID customerId) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getBalance(),
                account.getCurrency(), account.getStatus(), account.getCustomer().getId());
    }
}
