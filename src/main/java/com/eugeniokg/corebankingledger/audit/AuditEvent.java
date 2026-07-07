package com.eugeniokg.corebankingledger.audit;

import java.util.Map;

/**
 * Published whenever a key operation happens (account created/status changed, transfer
 * completed/failed, login succeeded/failed), and picked up by {@link AuditEventListener} to
 * write a durable {@link AuditLog} entry - see the {@code audit} package for details.
 */
public record AuditEvent(String username, String action, String entityType, String entityId,
                          Map<String, Object> details) {
}
