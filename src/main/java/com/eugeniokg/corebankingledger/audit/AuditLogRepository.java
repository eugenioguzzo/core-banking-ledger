package com.eugeniokg.corebankingledger.audit;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deliberately extends the bare {@link Repository} marker interface, not {@code JpaRepository},
 * and declares only {@code save} (always an insert, since entries are never re-saved with an
 * existing id) plus read methods. No update or delete method is declared anywhere for
 * {@link AuditLog} - the audit trail cannot be altered or removed from within the application.
 */
public interface AuditLogRepository extends Repository<AuditLog, UUID> {

    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    List<AuditLog> findAll();

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}
