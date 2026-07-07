package com.eugeniokg.corebankingledger.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The single, centralized place where {@link AuditEvent}s become {@link AuditLog} rows.
 * Uses {@code fallbackExecution = true} so that:
 * - an event published from within an ongoing @Transactional method (e.g. account creation)
 *   is only recorded after that transaction actually commits, never for a change that was
 *   rolled back;
 * - an event published with no ongoing transaction (e.g. a failed login, or a transfer
 *   failure raised after its own transaction already rolled back) is still recorded
 *   immediately, instead of being silently dropped.
 * A failure to record an audit entry is logged but never allowed to fail the request that
 * triggered it.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onAuditEvent(AuditEvent event) {
        try {
            auditService.record(event);
        } catch (RuntimeException e) {
            log.error("Failed to record audit log entry for action '{}' on entity type '{}'",
                    event.action(), event.entityType(), e);
        }
    }
}
