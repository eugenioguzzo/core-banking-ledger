package com.eugeniokg.corebankingledger.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Gives service-layer code access to the authenticated principal, so that ownership and
 * role checks can be enforced there too, not only at the controller/endpoint level.
 */
@Component
public class CurrentUserProvider {

    public AuthenticatedPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user in the current security context");
        }
        return principal;
    }
}
