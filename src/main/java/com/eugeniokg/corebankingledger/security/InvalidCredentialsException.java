package com.eugeniokg.corebankingledger.security;

/**
 * Thrown for any login failure. Always carries the same message regardless of the actual
 * cause (unknown email, wrong password, disabled account) so a failed login never reveals
 * whether a given email is registered.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
