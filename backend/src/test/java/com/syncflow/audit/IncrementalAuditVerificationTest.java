package com.syncflow.audit;

import com.syncflow.audit.application.port.out.AuditLogRepositoryPort;
import com.syncflow.audit.application.service.AuditHashChain;
import com.syncflow.audit.application.service.AuditLogApplicationService;
import com.syncflow.model.AuditChainCheckpoint;
import com.syncflow.model.AuditLog;
import com.syncflow.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Metrics verify only records appended since the stored checkpoint; a full verification re-walks
 * the whole ledger and is what detects edits to records that were already verified.
 */
class IncrementalAuditVerificationTest {

    private final List<AuditLog> ledger = new ArrayList<>();
    private AuditChainCheckpoint stored;
    private AuditLogRepositoryPort port;
    private AuditLogApplicationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        port = mock(AuditLogRepositoryPort.class);
        when(port.findCheckpoint()).thenAnswer(inv -> Optional.ofNullable(stored));
        when(port.saveCheckpoint(any())).thenAnswer(inv -> stored = inv.getArgument(0));
        when(port.findChainPage(anyLong(), anyInt())).thenAnswer(inv -> {
            long afterId = inv.getArgument(0);
            return ledger.stream().filter(l -> l.getId() > afterId).toList();
        });
        service = new AuditLogApplicationService(port, mock(CurrentUser.class), mock(ObjectProvider.class));
        append(3);
    }

    private void append(int count) {
        for (int i = 0; i < count; i++) {
            String previous = ledger.isEmpty() ? null : ledger.get(ledger.size() - 1).getVerificationHash();
            AuditLog log = AuditLog.builder()
                    .id((long) ledger.size() + 1)
                    .action("TASK_MOVED")
                    .entityType("TASK")
                    .actorName("Tester")
                    .details("event " + ledger.size())
                    .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .build();
            log.setPreviousHash(previous);
            log.setVerificationHash(AuditHashChain.hash(log, previous));
            ledger.add(log);
        }
    }

    @Test
    void metricsOnlyWalkRecordsAfterTheCheckpoint() {
        Map<String, Object> first = service.getAuditMetrics();
        assertThat(first.get("chainVerifiedRecords")).isEqualTo(3L);
        assertThat(stored.getLastVerifiedId()).isEqualTo(3L);

        append(2);
        Map<String, Object> second = service.getAuditMetrics();

        // Only the first pass started from the beginning of the ledger.
        verify(port, times(1)).findChainPage(eq(0L), anyInt());
        assertThat(second.get("chainVerifiedRecords")).isEqualTo(5L);
        assertThat(second.get("complianceStatus")).isEqualTo("CHAIN INTACT");
    }

    @Test
    void fullVerificationDetectsEditsToAlreadyVerifiedRecords() {
        service.getAuditMetrics();
        ledger.get(1).setDetails("history rewritten");

        // Incremental check does not revisit verified records...
        assertThat(service.getAuditMetrics().get("complianceStatus")).isEqualTo("CHAIN INTACT");

        // ...the full verification does.
        Map<String, Object> full = service.verifyFullChain();
        assertThat(full.get("complianceStatus")).isEqualTo("TAMPERING DETECTED (1)");
        assertThat(full.get("firstBrokenRecordId")).isEqualTo(2L);
        assertThat(full.get("lastFullVerificationAt")).isNotNull();
    }
}
