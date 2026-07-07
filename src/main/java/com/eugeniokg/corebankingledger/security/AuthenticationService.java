package com.eugeniokg.corebankingledger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthenticationService(AuthenticationManager authenticationManager, UserRepository userRepository,
                                  JwtService jwtService, JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }

        // The user is guaranteed to exist and be enabled at this point, since authentication
        // just succeeded against it.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidTokenException();
        }

        User user = userRepository.findByEmail(claims.getSubject())
                .filter(User::isEnabled)
                .orElseThrow(InvalidTokenException::new);

        String newAccessToken = jwtService.generateAccessToken(user);
        return new TokenResponse(newAccessToken, request.refreshToken(), "Bearer",
                jwtProperties.getAccessTokenTtl().toSeconds());
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtProperties.getAccessTokenTtl().toSeconds());
    }
}
