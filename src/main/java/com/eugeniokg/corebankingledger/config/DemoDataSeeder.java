package com.eugeniokg.corebankingledger.config;

import com.eugeniokg.corebankingledger.account.Account;
import com.eugeniokg.corebankingledger.account.AccountRepository;
import com.eugeniokg.corebankingledger.account.AccountStatus;
import com.eugeniokg.corebankingledger.account.Customer;
import com.eugeniokg.corebankingledger.account.CustomerRepository;
import com.eugeniokg.corebankingledger.security.Role;
import com.eugeniokg.corebankingledger.security.User;
import com.eugeniokg.corebankingledger.security.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Seeds a handful of clearly-fake demo customers, accounts and users so the public
 * showcase deployment is immediately explorable via Swagger UI, without anyone having to
 * create data by hand first. Runs once: it checks for the demo admin user first and does
 * nothing if it is already present, so restarting or redeploying never duplicates data.
 *
 * <p>Only active under the "prod" profile - this is demo data for the live deployment, not
 * something that should ever run against a real database. The credentials below are
 * intentionally public; see the "Live Demo" section of the README.
 */
@Component
@Profile("prod")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_PASSWORD = "DemoPass123!";
    private static final String DEMO_ADMIN_EMAIL = "admin.demo@example.com";
    private static final String DEMO_CUSTOMER_EMAIL = "alice.demo@example.com";

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(CustomerRepository customerRepository, AccountRepository accountRepository,
                           UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail(DEMO_ADMIN_EMAIL).isPresent()) {
            log.info("Demo data already present, skipping seeding.");
            return;
        }

        log.warn("Seeding fake demo data (customers, accounts, users) for the public showcase "
                + "deployment - see the README's 'Live Demo' section for credentials.");

        Customer alice = demoCustomer("Alice", "Demo", "DEMO-TAX-0000001", DEMO_CUSTOMER_EMAIL);
        demoAccount(alice, "DEMO-ACC-0000000001", new BigDecimal("1000.00"));
        demoUser(DEMO_CUSTOMER_EMAIL, Role.CUSTOMER, alice.getId());

        Customer bob = demoCustomer("Bob", "Demo", "DEMO-TAX-0000002", "bob.demo@example.com");
        demoAccount(bob, "DEMO-ACC-0000000002", new BigDecimal("500.00"));

        // Lets a Swagger UI visitor also try the staff-only endpoints (create account,
        // change account status, create/promote users) without needing real credentials.
        demoUser(DEMO_ADMIN_EMAIL, Role.ADMIN, null);
    }

    private Customer demoCustomer(String firstName, String lastName, String taxId, String email) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setTaxId(taxId);
        customer.setEmail(email);
        return customerRepository.save(customer);
    }

    private void demoAccount(Customer customer, String accountNumber, BigDecimal balance) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(accountNumber);
        account.setBalance(balance);
        account.setCurrency("EUR");
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
    }

    private void demoUser(String email, Role role, UUID customerId) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setCustomerId(customerId);
        user.setEnabled(true);
        userRepository.save(user);
    }
}
