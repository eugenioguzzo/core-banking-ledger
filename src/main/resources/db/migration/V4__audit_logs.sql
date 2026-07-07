-- Immutable audit trail. No update or delete is ever performed against this table by the
-- application (see AuditLogRepository) - only inserts.

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   VARCHAR(255),
    occurred_at TIMESTAMP    NOT NULL DEFAULT now(),
    details     JSONB
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
