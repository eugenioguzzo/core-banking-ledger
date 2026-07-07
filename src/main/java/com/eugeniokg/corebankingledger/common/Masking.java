package com.eugeniokg.corebankingledger.common;

/**
 * Masks sensitive values before they are written anywhere they might be persisted, logged
 * or otherwise retained outside of the immediate request that needed the real value.
 */
public final class Masking {

    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    private Masking() {
    }

    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= VISIBLE_SUFFIX_LENGTH) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - VISIBLE_SUFFIX_LENGTH);
    }
}
