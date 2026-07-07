package com.eugeniokg.corebankingledger.security;

/**
 * Distinguishes access tokens from refresh tokens so one can never be used in place
 * of the other, even though both are signed with the same key.
 */
enum TokenType {
    ACCESS,
    REFRESH
}
