package com.eugeniokg.corebankingledger;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base for integration tests that need a full Spring context backed by a real
 * PostgreSQL instance started with Testcontainers.
 *
 * <p>The container is started once, in a static initializer, and deliberately never stopped
 * by JUnit (the "singleton container" pattern) - it is NOT annotated with {@code @Container}/
 * {@code @Testcontainers}. All subclasses share the exact same Spring configuration, so the
 * test framework reuses a single cached {@code ApplicationContext} (and the datasource
 * properties resolved into it by {@link #datasourceProperties}) across every test class. If
 * the container were instead managed per-class (the more common {@code @Container} pattern),
 * JUnit would stop and restart it - on a new random port - between test classes, while the
 * cached Spring context kept using the now-stale, already-closed port from the first class,
 * failing every subsequent test class with a connection refused error. Ryuk cleans up the
 * container when the JVM exits.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestSupport {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("core_banking_ledger")
            .withUsername("ledger")
            .withPassword("ledger");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
