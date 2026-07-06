package com.eugeniokg.corebankingledger.common;

/**
 * Base type for exceptions signaling that a requested resource does not exist.
 */
public abstract class ResourceNotFoundException extends RuntimeException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
