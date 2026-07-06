package com.eugeniokg.corebankingledger.account;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    public Customer create(Customer customer) {
        return customerRepository.save(customer);
    }
}
