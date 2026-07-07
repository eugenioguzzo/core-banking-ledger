package com.eugeniokg.corebankingledger.transaction;

import com.eugeniokg.corebankingledger.account.AccountService;
import com.eugeniokg.corebankingledger.audit.AuditEvent;
import com.eugeniokg.corebankingledger.security.CurrentUserProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
    private final AccountService accountService;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
                               LedgerTransferExecutor ledgerTransferExecutor,
                               TransactionRetryProperties retryProperties,
                               AccountService accountService,
                               CurrentUserProvider currentUserProvider,
                               ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.ledgerTransferExecutor = ledgerTransferExecutor;
        this.retryProperties = retryProperties;
        this.accountService = accountService;
        this.currentUserProvider = currentUserProvider;
        this.eventPublisher = eventPublisher;
    }

    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {
        // Checked unconditionally, even for a request that turns out to be a repeated
        // idempotency key, so a customer can never fish for another customer's transfer
        // result by guessing an idempotency key.
        accountService.findAccessibleById(request.sourceAccountId());

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransferResponse::from)
                .orElseGet(() -> TransferResponse.from(executeWithRetry(request, idempotencyKey)));
    }

    private Transaction executeWithRetry(TransferRequest request, String idempotencyKey) {
        Duration backoff = retryProperties.getInitialBackoff();

        for (int attempt = 1; attempt <= retryProperties.getMaxAttempts(); attempt++) {
            try {
                Transaction transaction = ledgerTransferExecutor.execute(request, idempotencyKey);
                publishTransferAudit("TRANSFER_COMPLETED", transaction.getId().toString(), request, null);
                return transaction;
            } catch (DataIntegrityViolationException duplicateKey) {
                // A concurrent request with the same idempotency key completed first.
                return transactionRepository.findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> duplicateKey);
            } catch (InsufficientBalanceException insufficientBalance) {
                publishTransferAudit("TRANSFER_FAILED", null, request, insufficientBalance.getMessage());
                throw insufficientBalance;
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

    private void publishTransferAudit(String action, String transactionId, TransferRequest request, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("sourceAccountId", request.sourceAccountId().toString());
        details.put("destinationAccountId", request.destinationAccountId().toString());
        details.put("amount", request.amount().toString());
        if (reason != null) {
            details.put("reason", reason);
        }
        eventPublisher.publishEvent(new AuditEvent(currentUserProvider.getCurrentUser().getUsername(),
                action, "Transaction", transactionId, details));
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
