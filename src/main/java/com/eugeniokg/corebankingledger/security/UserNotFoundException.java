package com.eugeniokg.corebankingledger.security;

import com.eugeniokg.corebankingledger.common.ResourceNotFoundException;

import java.util.UUID;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }
}
