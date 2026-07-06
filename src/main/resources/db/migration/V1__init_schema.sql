-- Initial schema for the account/customer domain.

CREATE TABLE customers (
    id          UUID PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    tax_id      VARCHAR(32)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    customer_id     UUID         NOT NULL REFERENCES customers (id),
    account_number  VARCHAR(34)  NOT NULL UNIQUE,
    balance         NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency        VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
