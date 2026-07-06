package com.eugeniokg.corebankingledger.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void findByIdReturnsCustomerWhenPresent() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder().id(id).firstName("Jane").lastName("Doe").build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer result = customerService.findById(id);

        assertThat(result).isEqualTo(customer);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with id: " + id);
    }
}
