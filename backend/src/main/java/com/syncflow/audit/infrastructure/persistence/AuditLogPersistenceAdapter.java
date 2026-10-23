package com.syncflow.audit.infrastructure.persistence;

import com.syncflow.audit.application.port.out.AuditLogRepositoryPort;
import com.syncflow.model.AuditChainCheckpoint;
import com.syncflow.model.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogRepositoryPort {

    private final SpringDataAuditLogRepository repository;
    private final SpringDataAuditCheckpointRepository checkpointRepository;

    @Override
    public AuditLog save(AuditLog log) {
        return repository.save(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog appendChained(Consumer<String> sealWithPreviousHash, AuditLog log) {
        String previousHash = repository.findTopByOrderByIdDesc()
                .map(AuditLog::getVerificationHash)
                .orElse(null);
        sealWithPreviousHash.accept(previousHash);
        return repository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findChainPage(long afterId, int size) {
        return repository.findByIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, size));
    }

    @Override
    public Optional<AuditChainCheckpoint> findCheckpoint() {
        return checkpointRepository.findById(AuditChainCheckpoint.SINGLETON_ID);
    }

    @Override
    public AuditChainCheckpoint saveCheckpoint(AuditChainCheckpoint checkpoint) {
        return checkpointRepository.save(checkpoint);
    }

    @Override
    public List<AuditLog> getRecentLogs(int page, int size) {
        return repository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    @Override
    public List<AuditLog> getFilteredLogs(String action, String entityType, int page, int size) {
        return repository.findFiltered(action, entityType, PageRequest.of(page, size));
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
