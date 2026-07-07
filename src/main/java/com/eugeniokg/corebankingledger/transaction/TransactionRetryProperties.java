package com.eugeniokg.corebankingledger.transaction;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configures how a transfer is retried when it fails due to a concurrent update
 * (optimistic locking conflict) on one of the involved accounts.
 */
@ConfigurationProperties(prefix = "app.transaction.retry")
public class TransactionRetryProperties {

    /**
     * Maximum number of attempts (including the first one) before giving up.
     */
    private int maxAttempts = 5;

    /**
     * Delay before the first retry. Each subsequent retry multiplies this by {@link #backoffMultiplier}.
     */
    private Duration initialBackoff = Duration.ofMillis(50);

    private double backoffMultiplier = 2.0;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }
}
