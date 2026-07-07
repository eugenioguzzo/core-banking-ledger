package com.eugeniokg.corebankingledger.security;

import java.util.UUID;

public record UserResponse(UUID id, String email, Role role, UUID customerId, boolean enabled) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCustomerId(), user.isEnabled());
    }
}
