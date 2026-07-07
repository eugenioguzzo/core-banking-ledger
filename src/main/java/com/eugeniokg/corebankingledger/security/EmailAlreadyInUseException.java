package com.eugeniokg.corebankingledger.security;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException(String email) {
        super("A user with email " + email + " already exists");
    }
}
