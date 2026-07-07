package com.eugeniokg.corebankingledger.security;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {

    @Override
    public String toString() {
        return "TokenResponse[accessToken=****, refreshToken=****, tokenType=" + tokenType
                + ", expiresInSeconds=" + expiresInSeconds + "]";
    }
}
