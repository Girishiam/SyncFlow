package com.syncflow.audit;

import com.syncflow.audit.application.service.AuditHashChain;
import com.syncflow.model.AuditLog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditHashChainTest {

    private List<AuditLog> chain(int size) {
        List<AuditLog> logs = new ArrayList<>();
        String previous = null;
        for (int i = 1; i <= size; i++) {
            AuditLog log = AuditLog.builder()
                    .id((long) i)
                    .action("TASK_MOVED")
                    .entityType("TASK")
                    .entityId((long) i)
                    .actorName("Tester")
                    .details("move " + i)
                    .timestamp(Instant.now().truncatedTo(ChronoUnit.MICROS))
                    .build();
            log.setPreviousHash(previous);
            log.setVerificationHash(AuditHashChain.hash(log, previous));
            previous = log.getVerificationHash();
            logs.add(log);
        }
        return logs;
    }

    private AuditHashChain.VerificationResult verify(List<AuditLog> logs) {
        AuditHashChain.Verifier verifier = new AuditHashChain.Verifier();
        logs.forEach(verifier::accept);
        return verifier.result();
    }

    @Test
    void intactChainVerifies() {
        AuditHashChain.VerificationResult result = verify(chain(5));
        assertThat(result.verifiedRecords()).isEqualTo(5);
        assertThat(result.brokenRecords()).isZero();
    }

    @Test
    void editedRecordIsDetected() {
        List<AuditLog> logs = chain(5);
        logs.get(2).setDetails("rewritten history");
        AuditHashChain.VerificationResult result = verify(logs);
        assertThat(result.brokenRecords()).isEqualTo(1);
        assertThat(result.firstBrokenId()).isEqualTo(3L);
    }

    @Test
    void deletedRecordBreaksTheNextLink() {
        List<AuditLog> logs = chain(5);
        logs.remove(1);
        AuditHashChain.VerificationResult result = verify(logs);
        assertThat(result.brokenRecords()).isEqualTo(1);
        assertThat(result.firstBrokenId()).isEqualTo(3L);
    }

    @Test
    void legacyRecordsAreCountedSeparately() {
        AuditLog legacy = AuditLog.builder().id(1L).action("X").entityType("T").actorName("a")
                .verificationHash("A9F23B81C4E7D012").timestamp(Instant.now()).build();
        AuditHashChain.Verifier verifier = new AuditHashChain.Verifier();
        verifier.accept(legacy);
        assertThat(verifier.result().legacyRecords()).isEqualTo(1);
        assertThat(verifier.result().brokenRecords()).isZero();
    }
}
