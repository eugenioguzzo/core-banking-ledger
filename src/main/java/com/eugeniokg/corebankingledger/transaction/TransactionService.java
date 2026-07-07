package com.eugeniokg.corebankingledger.transaction;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Entry point for executing transfers. Handles idempotency (a repeated Idempotency-Key
 * returns the original result instead of re-executing the transfer) and retries the
 * transfer when it fails because another concurrent transfer changed one of the accounts
 * (optimistic locking conflict).
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerTransferExecutor ledgerTransferExecutor;
    private final TransactionRetryProperties retryProperties;

    public TransactionService(TransactionRepository transactionRepository,
                               LedgerTransferExecutor ledgerTransferExecutor,
                               TransactionRetryProperties retryProperties) {
        this.transactionRepository = transactionRepository;
        this.ledgerTransferExecutor = ledgerTransferExecutor;
        this.retryProperties = retryProperties;
    }

    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransferResponse::from)
                .orElseGet(() -> TransferResponse.from(executeWithRetry(request, idempotencyKey)));
    }

    private Transaction executeWithRetry(TransferRequest request, String idempotencyKey) {
        Duration backoff = retryProperties.getInitialBackoff();

        for (int attempt = 1; attempt <= retryProperties.getMaxAttempts(); attempt++) {
            try {
                return ledgerTransferExecutor.execute(request, idempotencyKey);
            } catch (DataIntegrityViolationException duplicateKey) {
                // A concurrent request with the same idempotency key completed first.
                return transactionRepository.findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> duplicateKey);
            } catch (ObjectOptimisticLockingFailureException conflict) {
                if (attempt == retryProperties.getMaxAttempts()) {
                    throw conflict;
                }
                sleep(backoff);
                backoff = Duration.ofMillis((long) (backoff.toMillis() * retryProperties.getBackoffMultiplier()));
            }
        }

        throw new IllegalStateException("Transfer retry loop exited without a result");
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying a transfer", e);
        }
    }
}
