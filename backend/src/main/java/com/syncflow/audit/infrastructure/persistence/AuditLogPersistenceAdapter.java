package com.syncflow.audit.infrastructure.persistence;

import com.syncflow.audit.application.port.out.AuditLogRepositoryPort;
import com.syncflow.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogRepositoryPort {

    private final SpringDataAuditLogRepository repository;

    @Override
    public AuditLog save(AuditLog log) {
        return repository.save(log);
    }

    @Override
    public List<AuditLog> getRecentLogs(Pageable pageable) {
        return repository.findAllByOrderByTimestampDesc(pageable);
    }

    @Override
    public List<AuditLog> getFilteredLogs(String action, String entityType, Pageable pageable) {
        return repository.findFiltered(action, entityType, pageable);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long countByActionStartingWith(String prefix) {
        return repository.countByActionStartingWith(prefix);
    }
}
