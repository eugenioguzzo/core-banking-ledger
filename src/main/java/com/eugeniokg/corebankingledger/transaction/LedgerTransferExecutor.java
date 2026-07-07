package com.eugeniokg.corebankingledger.transaction;

import com.eugeniokg.corebankingledger.account.Account;
import com.eugeniokg.corebankingledger.account.AccountNotFoundException;
import com.eugeniokg.corebankingledger.account.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Executes a single transfer attempt atomically: validates the balance, records the two
 * matching ledger entries (double-entry bookkeeping) and updates the cached account balances.
 * Never called directly by clients - {@link TransactionService} wraps it with idempotency
 * handling and retries on optimistic locking conflicts.
 */
@Service
public class LedgerTransferExecutor {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerTransferExecutor(AccountRepository accountRepository,
                                   TransactionRepository transactionRepository,
                                   LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public Transaction execute(TransferRequest request, String idempotencyKey) {
        Account source = accountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.sourceAccountId()));
        Account destination = accountRepository.findById(request.destinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.destinationAccountId()));

        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException(source.getId(), source.getBalance(), request.amount());
        }

        Transaction transaction = new Transaction();
        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setAmount(request.amount());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setDescription(request.description());
        // Flushed immediately so a duplicate idempotency key fails fast, before any ledger
        // entry or balance change is made for this attempt.
        transaction = transactionRepository.saveAndFlush(transaction);

        Instant now = Instant.now();
        LedgerEntry debit = new LedgerEntry(null, source, transaction.getId(), EntryType.DEBIT, request.amount(), now);
        LedgerEntry credit = new LedgerEntry(null, destination, transaction.getId(), EntryType.CREDIT, request.amount(), now);
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        source.setBalance(source.getBalance().subtract(request.amount()));
        destination.setBalance(destination.getBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(destination);

        return transaction;
    }
}
