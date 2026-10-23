package com.syncflow.audit.application.port.in;

import com.syncflow.dto.AuditLogDTO;

import java.util.List;
import java.util.Map;

public interface AuditLogUseCase {
    AuditLogDTO recordLog(String action, String entityType, Long entityId, String entityName,
                         String actorName, String actorEmail, String actorRole, String details);

    List<AuditLogDTO> getAuditLogs(String action, String entityType, int page, int size);
    
    /** Metrics with incremental hash-chain verification. */
    Map<String, Object> getAuditMetrics();

    /** Re-verifies the entire hash chain from the first record. */
    Map<String, Object> verifyFullChain();

    byte[] exportAuditLogsCsv(String action, String entityType);
}
