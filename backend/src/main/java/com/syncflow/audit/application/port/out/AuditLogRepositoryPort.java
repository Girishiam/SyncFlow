package com.syncflow.audit.application.port.out;

import com.syncflow.model.AuditLog;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditLogRepositoryPort {
    AuditLog save(AuditLog log);
    List<AuditLog> getRecentLogs(Pageable pageable);
    List<AuditLog> getFilteredLogs(String action, String entityType, Pageable pageable);
    long count();
    long countByActionStartingWith(String prefix);
}
