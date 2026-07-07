package com.eugeniokg.corebankingledger.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID accountId, BigDecimal available, BigDecimal requested) {
        super("Account " + accountId + " has insufficient balance: available " + available
                + ", requested " + requested);
    }
}
