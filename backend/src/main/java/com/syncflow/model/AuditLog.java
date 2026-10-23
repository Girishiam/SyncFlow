package com.syncflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action; // e.g. TASK_CREATED, TASK_MOVED, TASK_COMPLETED, TASK_BLOCKED, TASK_ASSIGNED, ROLE_UPDATED

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // TASK, ROLE, CHANNEL, PROJECT

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 255)
    private String entityName;

    @Column(name = "actor_name", nullable = false, length = 150)
    private String actorName;

    @Column(name = "actor_email", length = 150)
    private String actorEmail;

    @Column(name = "actor_role", length = 100)
    private String actorRole;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String details;

    @Column(name = "ip_address", length = 50)
    @Builder.Default
    private String ipAddress = "127.0.0.1";

    /** SHA-256 over this record's content chained to {@link #previousHash}. */
    @Column(name = "verification_hash", length = 64)
    private String verificationHash;

    /** Hash of the preceding record; null for records created before hash chaining was introduced. */
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
