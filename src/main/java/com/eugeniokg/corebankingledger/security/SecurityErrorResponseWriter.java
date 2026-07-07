package com.eugeniokg.corebankingledger.security;

import com.eugeniokg.corebankingledger.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Writes a JSON {@link ErrorResponse} directly to the servlet response. Used by the security
 * filter chain's entry point and access-denied handler, which run before Spring MVC's own
 * exception handling machinery and so cannot rely on {@code @RestControllerAdvice}.
 */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
