package com.eugeniokg.corebankingledger.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull Role role,
        UUID customerId
) {

    @Override
    public String toString() {
        return "CreateUserRequest[email=" + email + ", password=****, role=" + role
                + ", customerId=" + customerId + "]";
    }
}
