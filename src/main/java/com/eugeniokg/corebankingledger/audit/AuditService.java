package com.eugeniokg.corebankingledger.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Runs in its own, always-committing transaction so an audit entry is recorded even when
     * the business operation that triggered it fails and rolls back (e.g. a failed login or a
     * transfer rejected for insufficient balance).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        auditLogRepository.save(new AuditLog(event.username(), event.action(), event.entityType(),
                event.entityId(), event.details()));
    }
}
