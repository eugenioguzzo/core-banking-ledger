package com.eugeniokg.corebankingledger.account;

import com.eugeniokg.corebankingledger.common.ResourceNotFoundException;

import java.util.UUID;

public class AccountNotFoundException extends ResourceNotFoundException {

    public AccountNotFoundException(UUID id) {
        super("Account not found with id: " + id);
    }
}
