package com.eugeniokg.corebankingledger.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Builds the "prod" datasource from whatever connection details the hosting provider makes
 * available as environment variables, preferring a single {@code DATABASE_URL} (the form
 * Render - and other Heroku-style providers - hand out for their managed Postgres add-on:
 * {@code postgres://user:password@host:port/database}) and falling back to separate
 * {@code DB_HOST}/{@code DB_PORT}/{@code DB_NAME}/{@code DB_USER}/{@code DB_PASSWORD} variables
 * otherwise.
 *
 * <p>This cannot be done with a plain {@code spring.datasource.url} placeholder: the
 * PostgreSQL JDBC driver does not accept credentials embedded in the URL's user-info
 * component, only a plain {@code jdbc:postgresql://host:port/database} URL with the
 * username/password supplied separately - so {@code DATABASE_URL} has to be parsed first.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    @Bean
    public DataSource dataSource(Environment env) {
        JdbcCredentials credentials = resolveCredentials(env);
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(credentials.jdbcUrl());
        dataSource.setUsername(credentials.username());
        dataSource.setPassword(credentials.password());
        return dataSource;
    }

    static JdbcCredentials resolveCredentials(Environment env) {
        String databaseUrl = env.getProperty("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            return fromDatabaseUrl(databaseUrl);
        }

        String host = env.getRequiredProperty("DB_HOST");
        String port = env.getProperty("DB_PORT", "5432");
        String database = env.getRequiredProperty("DB_NAME");
        String username = env.getRequiredProperty("DB_USER");
        String password = env.getRequiredProperty("DB_PASSWORD");
        return new JdbcCredentials("jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);
    }

    static JdbcCredentials fromDatabaseUrl(String databaseUrl) {
        URI uri = URI.create(databaseUrl.replaceFirst("^postgres(ql)?://", "postgresql://"));
        String userInfo = uri.getUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalStateException(
                    "DATABASE_URL must include credentials, in the form postgres://user:password@host:port/database");
        }

        int separator = userInfo.indexOf(':');
        String username = userInfo.substring(0, separator);
        String password = userInfo.substring(separator + 1);
        // URI.getPort() returns -1 when the URL has no explicit port (Render's internal
        // database hostname, for example) - PostgreSQL's default port applies in that case.
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
        return new JdbcCredentials(jdbcUrl, username, password);
    }

    record JdbcCredentials(String jdbcUrl, String username, String password) {
    }
}
