package com.eugeniokg.corebankingledger.account;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    // Fetches the customer in the same query, avoiding one extra query per account
    // (N+1) when callers access account.getCustomer() for each result.
    @EntityGraph(attributePaths = "customer")
    List<Account> findByCustomerId(UUID customerId);
}
