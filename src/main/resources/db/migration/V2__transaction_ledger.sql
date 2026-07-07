-- Adds optimistic locking to accounts and introduces the transaction/ledger domain.

ALTER TABLE accounts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE transactions (
    id                      UUID PRIMARY KEY,
    source_account_id       UUID          NOT NULL REFERENCES accounts (id),
    destination_account_id UUID          NOT NULL REFERENCES accounts (id),
    amount                  NUMERIC(19,4) NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    idempotency_key         VARCHAR(128)  NOT NULL UNIQUE,
    description             VARCHAR(255),
    occurred_at             TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries (
    id             UUID PRIMARY KEY,
    account_id     UUID          NOT NULL REFERENCES accounts (id),
    transaction_id UUID          NOT NULL REFERENCES transactions (id),
    type           VARCHAR(10)   NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    occurred_at    TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_source_account_id ON transactions (source_account_id);
CREATE INDEX idx_transactions_destination_account_id ON transactions (destination_account_id);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries (account_id);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);
