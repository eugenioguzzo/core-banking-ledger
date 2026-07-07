package com.eugeniokg.corebankingledger.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdDataSourceConfigTest {

    @Test
    void parsesRenderStyleDatabaseUrl() {
        var credentials = ProdDataSourceConfig.fromDatabaseUrl(
                "postgres://ledger:s3cr3t@dpg-example-a.oregon-postgres.render.com:5432/core_banking_ledger");

        assertThat(credentials.jdbcUrl())
                .isEqualTo("jdbc:postgresql://dpg-example-a.oregon-postgres.render.com:5432/core_banking_ledger");
        assertThat(credentials.username()).isEqualTo("ledger");
        assertThat(credentials.password()).isEqualTo("s3cr3t");
    }

    @Test
    void rejectsDatabaseUrlWithoutCredentials() {
        assertThatThrownBy(() -> ProdDataSourceConfig.fromDatabaseUrl("postgres://host:5432/db"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prefersDatabaseUrlOverSeparateVariables() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://ledger:s3cr3t@example-host:5432/core_banking_ledger")
                .withProperty("DB_HOST", "should-not-be-used");

        var credentials = ProdDataSourceConfig.resolveCredentials(env);

        assertThat(credentials.jdbcUrl()).isEqualTo("jdbc:postgresql://example-host:5432/core_banking_ledger");
    }

    @Test
    void fallsBackToSeparateVariablesWhenDatabaseUrlIsAbsent() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DB_HOST", "localhost")
                .withProperty("DB_PORT", "5433")
                .withProperty("DB_NAME", "core_banking_ledger")
                .withProperty("DB_USER", "ledger")
                .withProperty("DB_PASSWORD", "ledger");

        var credentials = ProdDataSourceConfig.resolveCredentials(env);

        assertThat(credentials.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5433/core_banking_ledger");
        assertThat(credentials.username()).isEqualTo("ledger");
        assertThat(credentials.password()).isEqualTo("ledger");
    }
}
