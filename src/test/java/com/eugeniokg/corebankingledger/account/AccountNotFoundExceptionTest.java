package com.eugeniokg.corebankingledger.account;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNotFoundExceptionTest {

    @Test
    void messageContainsAccountId() {
        UUID id = UUID.randomUUID();

        AccountNotFoundException exception = new AccountNotFoundException(id);

        assertThat(exception.getMessage()).isEqualTo("Account not found with id: " + id);
    }
}
