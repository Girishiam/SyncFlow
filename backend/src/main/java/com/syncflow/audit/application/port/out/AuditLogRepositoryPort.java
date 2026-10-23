package com.syncflow.audit.application.port.out;

import com.syncflow.model.AuditChainCheckpoint;
import com.syncflow.model.AuditLog;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface AuditLogRepositoryPort {
    AuditLog save(AuditLog log);

    /**
     * Appends a record to the tamper-evident chain in its own transaction. The callback receives the
     * hash of the current chain tail (or {@code null} for the first record) and seals {@code log}
     * with its hashes before it is saved.
     */
    AuditLog appendChained(Consumer<String> sealWithPreviousHash, AuditLog log);

    /** Records with id greater than {@code afterId}, ascending; used to walk the chain. */
    List<AuditLog> findChainPage(long afterId, int size);

    Optional<AuditChainCheckpoint> findCheckpoint();
    AuditChainCheckpoint saveCheckpoint(AuditChainCheckpoint checkpoint);

    /** Newest first; {@code page} is zero based. */
    List<AuditLog> getRecentLogs(int page, int size);
    /** Newest first, filtered by action and/or entity type (null = any). */
    List<AuditLog> getFilteredLogs(String action, String entityType, int page, int size);
    long count();
    long countByActionStartingWith(String prefix);
}
