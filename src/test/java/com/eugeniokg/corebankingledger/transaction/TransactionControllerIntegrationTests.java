package com.eugeniokg.corebankingledger.transaction;

import com.eugeniokg.corebankingledger.IntegrationTestSupport;
import com.eugeniokg.corebankingledger.account.Account;
import com.eugeniokg.corebankingledger.account.AccountRepository;
import com.eugeniokg.corebankingledger.account.AccountStatus;
import com.eugeniokg.corebankingledger.account.Customer;
import com.eugeniokg.corebankingledger.account.CustomerRepository;
import com.eugeniokg.corebankingledger.common.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionControllerIntegrationTests extends IntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void concurrentTransfersFromSameAccountDoNotLoseUpdates() throws Exception {
        Account source = createAccount(new BigDecimal("1000.00"));
        Account destinationA = createAccount(BigDecimal.ZERO);
        Account destinationB = createAccount(BigDecimal.ZERO);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<ResponseEntity<TransferResponse>> transferToA = () -> {
            ready.countDown();
            start.await();
            return postTransfer(source.getId(), destinationA.getId(), new BigDecimal("100.00"),
                    "concurrent transfer to A", "concurrent-key-a-" + UUID.randomUUID());
        };
        Callable<ResponseEntity<TransferResponse>> transferToB = () -> {
            ready.countDown();
            start.await();
            return postTransfer(source.getId(), destinationB.getId(), new BigDecimal("200.00"),
                    "concurrent transfer to B", "concurrent-key-b-" + UUID.randomUUID());
        };

        Future<ResponseEntity<TransferResponse>> futureA = executor.submit(transferToA);
        Future<ResponseEntity<TransferResponse>> futureB = executor.submit(transferToB);
        ready.await();
        start.countDown();

        ResponseEntity<TransferResponse> responseA = futureA.get(15, TimeUnit.SECONDS);
        ResponseEntity<TransferResponse> responseB = futureB.get(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account reloadedDestinationA = accountRepository.findById(destinationA.getId()).orElseThrow();
        Account reloadedDestinationB = accountRepository.findById(destinationB.getId()).orElseThrow();

        assertThat(reloadedSource.getBalance()).isEqualByComparingTo("700.00");
        assertThat(reloadedDestinationA.getBalance()).isEqualByComparingTo("100.00");
        assertThat(reloadedDestinationB.getBalance()).isEqualByComparingTo("200.00");

        List<LedgerEntry> sourceEntries = ledgerEntryRepository.findByAccountId(source.getId());
        assertThat(sourceEntries).hasSize(2);
        assertThat(sourceEntries).allMatch(entry -> entry.getType() == EntryType.DEBIT);
    }

    @Test
    void duplicateIdempotencyKeyExecutesTransferOnlyOnce() {
        Account source = createAccount(new BigDecimal("500.00"));
        Account destination = createAccount(BigDecimal.ZERO);
        String idempotencyKey = "idempotency-key-" + UUID.randomUUID();

        ResponseEntity<TransferResponse> first = postTransfer(source.getId(), destination.getId(),
                new BigDecimal("50.00"), "first attempt", idempotencyKey);
        ResponseEntity<TransferResponse> second = postTransfer(source.getId(), destination.getId(),
                new BigDecimal("50.00"), "second attempt", idempotencyKey);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().id()).isEqualTo(first.getBody().id());
        assertThat(second.getBody().description()).isEqualTo("first attempt");

        Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
        Account reloadedDestination = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(reloadedSource.getBalance()).isEqualByComparingTo("450.00");
        assertThat(reloadedDestination.getBalance()).isEqualByComparingTo("50.00");

        long matchingTransactions = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getIdempotencyKey().equals(idempotencyKey))
                .count();
        assertThat(matchingTransactions).isEqualTo(1);
    }

    @Test
    void transferWithInsufficientBalanceFailsWithClearError() {
        Account source = createAccount(new BigDecimal("10.00"));
        Account destination = createAccount(BigDecimal.ZERO);

        ResponseEntity<ErrorResponse> response = postTransferExpectingError(source.getId(), destination.getId(),
                new BigDecimal("100.00"), "too much", "insufficient-key-" + UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("insufficient balance");

        Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(reloadedSource.getBalance()).isEqualByComparingTo("10.00");
        assertThat(ledgerEntryRepository.findByAccountId(source.getId())).isEmpty();
    }

    private Account createAccount(BigDecimal balance) {
        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer.setTaxId(UUID.randomUUID().toString().substring(0, 16));
        customer.setEmail(UUID.randomUUID() + "@example.com");
        customer = customerRepository.save(customer);

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(UUID.randomUUID().toString().substring(0, 20));
        account.setBalance(balance);
        account.setCurrency("EUR");
        account.setStatus(AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    private ResponseEntity<TransferResponse> postTransfer(UUID sourceAccountId, UUID destinationAccountId,
                                                            BigDecimal amount, String description, String idempotencyKey) {
        HttpEntity<TransferRequest> entity = requestEntity(sourceAccountId, destinationAccountId, amount,
                description, idempotencyKey);
        return restTemplate.postForEntity("/transactions", entity, TransferResponse.class);
    }

    private ResponseEntity<ErrorResponse> postTransferExpectingError(UUID sourceAccountId, UUID destinationAccountId,
                                                                      BigDecimal amount, String description,
                                                                      String idempotencyKey) {
        HttpEntity<TransferRequest> entity = requestEntity(sourceAccountId, destinationAccountId, amount,
                description, idempotencyKey);
        return restTemplate.postForEntity("/transactions", entity, ErrorResponse.class);
    }

    private HttpEntity<TransferRequest> requestEntity(UUID sourceAccountId, UUID destinationAccountId,
                                                        BigDecimal amount, String description, String idempotencyKey) {
        TransferRequest request = new TransferRequest(sourceAccountId, destinationAccountId, amount, description);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        return new HttpEntity<>(request, headers);
    }
}
