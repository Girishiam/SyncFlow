package com.syncflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Single-row progress marker for audit hash-chain verification. Records up to
 * {@link #lastVerifiedId} have been checked; later checks continue from {@link #lastHash}.
 */
@Entity
@Table(name = "audit_chain_checkpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditChainCheckpoint {

    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "last_verified_id", nullable = false)
    private Long lastVerifiedId;

    @Column(name = "last_hash", length = 64)
    private String lastHash;

    @Column(name = "verified_records", nullable = false)
    private Long verifiedRecords;

    @Column(name = "legacy_records", nullable = false)
    private Long legacyRecords;

    @Column(name = "broken_records", nullable = false)
    private Long brokenRecords;

    @Column(name = "first_broken_id")
    private Long firstBrokenId;

    @Column(name = "last_full_check_at")
    private Instant lastFullCheckAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AuditChainCheckpoint empty() {
        return AuditChainCheckpoint.builder()
                .id(SINGLETON_ID)
                .lastVerifiedId(0L)
                .verifiedRecords(0L)
                .legacyRecords(0L)
                .brokenRecords(0L)
                .updatedAt(Instant.now())
                .build();
    }
}
