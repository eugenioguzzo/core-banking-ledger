package com.eugeniokg.corebankingledger.security;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
}
