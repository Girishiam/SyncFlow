package com.syncflow.model;

import com.syncflow.model.enums.*;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "automation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "trigger_event", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AutomationTrigger triggerEvent;

    @Column(name = "action_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AutomationAction actionType;

    @Column(name = "target_role", length = 50)
    private String targetRole; // ROLE_SQA_ENGINEER, ROLE_PROJECT_MANAGER

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
