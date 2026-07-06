package com.eugeniokg.corebankingledger.account;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNotFoundExceptionTest {

    @Test
    void messageContainsCustomerId() {
        UUID id = UUID.randomUUID();

        CustomerNotFoundException exception = new CustomerNotFoundException(id);

        assertThat(exception.getMessage()).isEqualTo("Customer not found with id: " + id);
    }
}
