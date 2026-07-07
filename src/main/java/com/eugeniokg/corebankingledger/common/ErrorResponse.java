package com.eugeniokg.corebankingledger.common;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message) {
}
