package com.syncflow.audit.application.port.in;

import com.syncflow.dto.AuditLogDTO;

import java.util.List;
import java.util.Map;

public interface AuditLogUseCase {
    AuditLogDTO recordLog(String action, String entityType, Long entityId, String entityName,
                         String actorName, String actorEmail, String actorRole, String details);

    List<AuditLogDTO> getAuditLogs(String action, String entityType, int page, int size);
    
    Map<String, Object> getAuditMetrics();

    byte[] exportAuditLogsCsv(String action, String entityType);
}
