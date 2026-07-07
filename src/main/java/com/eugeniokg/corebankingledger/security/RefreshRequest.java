package com.eugeniokg.corebankingledger.security;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {

    @Override
    public String toString() {
        return "RefreshRequest[refreshToken=****]";
    }
}
