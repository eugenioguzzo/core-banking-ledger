package com.eugeniokg.corebankingledger.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        TransactionStatus status,
        String idempotencyKey,
        String description,
        Instant timestamp
) {

    public static TransferResponse from(Transaction transaction) {
        return new TransferResponse(
                transaction.getId(),
                transaction.getSourceAccount().getId(),
                transaction.getDestinationAccount().getId(),
                transaction.getAmount(),
                transaction.getStatus(),
                transaction.getIdempotencyKey(),
                transaction.getDescription(),
                transaction.getTimestamp()
        );
    }
}
