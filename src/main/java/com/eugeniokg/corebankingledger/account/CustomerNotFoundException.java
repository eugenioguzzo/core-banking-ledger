package com.eugeniokg.corebankingledger.account;

import com.eugeniokg.corebankingledger.common.ResourceNotFoundException;

import java.util.UUID;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(UUID id) {
        super("Customer not found with id: " + id);
    }
}
