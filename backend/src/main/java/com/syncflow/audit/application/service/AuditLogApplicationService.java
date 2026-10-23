package com.syncflow.audit.application.service;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.audit.application.port.out.AuditLogRepositoryPort;
import com.syncflow.dto.AuditLogDTO;
import com.syncflow.model.AuditChainCheckpoint;
import com.syncflow.model.AuditLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import com.syncflow.security.CurrentUser;
import com.syncflow.security.UserDetailsImpl;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogApplicationService implements AuditLogUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_EXPORT_ROWS = 10_000;

    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final CurrentUser currentUser;
    /** Self reference so the scheduled job goes through the transactional proxy. */
    private final ObjectProvider<AuditLogUseCase> selfProvider;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Records an audit entry. The actor and client IP are taken from the authenticated request when
     * there is one; the actor arguments are only used for system-initiated events (schedulers,
     * automations). Inside a transaction the entry is written after commit so that only completed
     * actions are audited and an audit failure can never roll back the business operation.
     */
    @Override
    public AuditLogDTO recordLog(String action, String entityType, Long entityId, String entityName,
                                 String actorName, String actorEmail, String actorRole, String details) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .entityName(entityName)
                .details(details)
                .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                .build();

        Optional<UserDetailsImpl> user = currentUser.get();
        if (user.isPresent()) {
            UserDetailsImpl u = user.get();
            entry.setActorName(u.getFullName());
            entry.setActorEmail(u.getEmail());
            entry.setActorRole(u.getUser().getRole() != null ? u.getUser().getRole().getName() : null);
        } else {
            entry.setActorName(actorName != null ? actorName : "SyncFlow System");
            entry.setActorEmail(actorEmail);
            entry.setActorRole(actorRole != null ? actorRole : "SYSTEM");
        }
        entry.setIpAddress(resolveClientIp());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persist(entry);
                }
            });
        } else {
            persist(entry);
        }
        return toDTO(entry);
    }

    private void persist(AuditLog entry) {
        try {
            auditLogRepositoryPort.appendChained(previousHash -> {
                entry.setPreviousHash(previousHash);
                entry.setVerificationHash(AuditHashChain.hash(entry, previousHash));
            }, entry);
            log.info("Audit [{}] {} #{} by {}", entry.getAction(), entry.getEntityType(), entry.getEntityId(), entry.getActorEmail() != null ? entry.getActorEmail() : entry.getActorName());
        } catch (Exception e) {
            log.error("Failed to persist audit record {} for {} #{}", entry.getAction(), entry.getEntityType(), entry.getEntityId(), e);
        }
    }

    private String resolveClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogs(String action, String entityType, int page, int size) {
        return query(action, entityType, Math.max(0, page), Math.max(1, Math.min(MAX_PAGE_SIZE, size)));
    }

    private List<AuditLogDTO> query(String action, String entityType, int page, int size) {
        boolean filtered = (action != null && !action.isBlank()) || (entityType != null && !entityType.isBlank());
        List<AuditLog> list = filtered
                ? auditLogRepositoryPort.getFilteredLogs(
                        action != null && !action.isBlank() ? action : null,
                        entityType != null && !entityType.isBlank() ? entityType : null,
                        page, size)
                : auditLogRepositoryPort.getRecentLogs(page, size);
        return list.stream().map(this::toDTO).toList();
    }

    /**
     * Ledger metrics. Chain verification is incremental: only records appended since the stored
     * checkpoint are checked here. {@link #verifyFullChain()} re-checks everything (nightly and on
     * demand), which is what detects edits to already-verified records.
     */
    @Override
    @Transactional
    public Map<String, Object> getAuditMetrics() {
        AuditChainCheckpoint checkpoint = auditLogRepositoryPort.findCheckpoint().orElseGet(AuditChainCheckpoint::empty);
        AuditHashChain.Verifier verifier = new AuditHashChain.Verifier(checkpoint.getLastHash(),
                checkpoint.getVerifiedRecords(), checkpoint.getBrokenRecords(), checkpoint.getLegacyRecords(),
                checkpoint.getFirstBrokenId());
        checkpoint = advance(checkpoint, verifier, checkpoint.getLastVerifiedId());
        return toMetrics(checkpoint);
    }

    @Override
    @Transactional
    public Map<String, Object> verifyFullChain() {
        AuditChainCheckpoint checkpoint = auditLogRepositoryPort.findCheckpoint().orElseGet(AuditChainCheckpoint::empty);
        checkpoint.setLastFullCheckAt(Instant.now());
        checkpoint = advance(checkpoint, new AuditHashChain.Verifier(), 0L);
        if (checkpoint.getBrokenRecords() > 0) {
            log.error("Audit hash chain verification found {} broken record(s); first at id {}",
                    checkpoint.getBrokenRecords(), checkpoint.getFirstBrokenId());
        }
        return toMetrics(checkpoint);
    }

    /** Nightly full re-verification of the ledger. */
    @Scheduled(cron = "0 30 2 * * *")
    public void scheduledFullVerification() {
        selfProvider.getObject().verifyFullChain();
    }

    /** Walks records after {@code afterId} in id order and stores the new checkpoint. */
    private AuditChainCheckpoint advance(AuditChainCheckpoint checkpoint, AuditHashChain.Verifier verifier, long afterId) {
        long cursor = afterId;
        while (true) {
            List<AuditLog> page = auditLogRepositoryPort.findChainPage(cursor, 500);
            if (page.isEmpty()) break;
            page.forEach(verifier::accept);
            cursor = page.get(page.size() - 1).getId();
        }
        AuditHashChain.VerificationResult result = verifier.result();
        checkpoint.setLastVerifiedId(cursor);
        checkpoint.setLastHash(verifier.lastId() != null ? verifier.lastHash() : checkpoint.getLastHash());
        checkpoint.setVerifiedRecords(result.verifiedRecords());
        checkpoint.setBrokenRecords(result.brokenRecords());
        checkpoint.setLegacyRecords(result.legacyRecords());
        checkpoint.setFirstBrokenId(result.firstBrokenId());
        checkpoint.setUpdatedAt(Instant.now());
        return auditLogRepositoryPort.saveCheckpoint(checkpoint);
    }

    private Map<String, Object> toMetrics(AuditChainCheckpoint chain) {
        String status;
        if (chain.getBrokenRecords() > 0) {
            status = "TAMPERING DETECTED (" + chain.getBrokenRecords() + ")";
        } else if (chain.getVerifiedRecords() == 0) {
            status = chain.getLegacyRecords() > 0 ? "LEGACY RECORDS ONLY" : "EMPTY LEDGER";
        } else {
            status = "CHAIN INTACT";
        }
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalAuditRecords", auditLogRepositoryPort.count());
        metrics.put("taskAuditRecords", auditLogRepositoryPort.countByActionStartingWith("TASK_"));
        metrics.put("rbacAuditRecords", auditLogRepositoryPort.countByActionStartingWith("ROLE_"));
        metrics.put("complianceStatus", status);
        metrics.put("integrityAlgorithm", "SHA-256 hash chain");
        metrics.put("chainVerifiedRecords", chain.getVerifiedRecords());
        metrics.put("chainBrokenRecords", chain.getBrokenRecords());
        metrics.put("legacyUnchainedRecords", chain.getLegacyRecords());
        metrics.put("firstBrokenRecordId", chain.getFirstBrokenId());
        metrics.put("lastFullVerificationAt", chain.getLastFullCheckAt());
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAuditLogsCsv(String action, String entityType) {
        List<AuditLogDTO> logs = query(action, entityType, 0, MAX_EXPORT_ROWS);

        StringBuilder sb = new StringBuilder();
        // RFC 4180 CSV Header
        sb.append("Log ID,Timestamp,Action,Entity Type,Entity ID,Entity Name,Operator,Operator Email,Operator Role,Audit Details,IP Address,Integrity Hash\n");

        for (AuditLogDTO l : logs) {
            sb.append(escapeCsv(String.valueOf(l.getId()))).append(",")
              .append(escapeCsv(l.getFormattedDate())).append(",")
              .append(escapeCsv(l.getAction())).append(",")
              .append(escapeCsv(l.getEntityType())).append(",")
              .append(escapeCsv(l.getEntityId() != null ? String.valueOf(l.getEntityId()) : "")).append(",")
              .append(escapeCsv(l.getEntityName())).append(",")
              .append(escapeCsv(l.getActorName())).append(",")
              .append(escapeCsv(l.getActorEmail())).append(",")
              .append(escapeCsv(l.getActorRole())).append(",")
              .append(escapeCsv(l.getDetails())).append(",")
              .append(escapeCsv(l.getIpAddress())).append(",")
              .append(escapeCsv(l.getVerificationHash())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        // Neutralise spreadsheet formula injection (=, +, -, @ and control prefixes).
        if (!val.isEmpty() && "=+-@\t\r".indexOf(val.charAt(0)) >= 0) {
            val = "'" + val;
        }
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }

    private AuditLogDTO toDTO(AuditLog a) {
        String avatar = "SF";
        if (a.getActorName() != null && !a.getActorName().isBlank()) {
            String[] parts = a.getActorName().trim().split("\\s+");
            if (parts.length >= 2) {
                avatar = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            } else {
                avatar = a.getActorName().substring(0, Math.min(2, a.getActorName().length())).toUpperCase();
            }
        }

        String timeAgo = "Just now";
        if (a.getTimestamp() != null) {
            Duration d = Duration.between(a.getTimestamp(), Instant.now());
            if (d.toDays() > 0) timeAgo = d.toDays() + "d ago";
            else if (d.toHours() > 0) timeAgo = d.toHours() + "h ago";
            else if (d.toMinutes() > 0) timeAgo = d.toMinutes() + "m ago";
        }

        return AuditLogDTO.builder()
                .id(a.getId())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .entityName(a.getEntityName())
                .actorName(a.getActorName())
                .actorEmail(a.getActorEmail())
                .actorRole(a.getActorRole())
                .actorAvatar(avatar)
                .details(a.getDetails())
                .ipAddress(a.getIpAddress())
                .verificationHash(a.getVerificationHash())
                .timestamp(a.getTimestamp())
                .formattedDate(a.getTimestamp() != null ? DATE_FORMATTER.format(a.getTimestamp()) : "")
                .timeAgo(timeAgo)
                .build();
    }
}
