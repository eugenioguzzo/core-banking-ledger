-- Login identities. CUSTOMER users are linked to a customer record; OPERATOR/ADMIN are staff.

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    customer_id   UUID REFERENCES customers (id),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_customer_id ON users (customer_id);
