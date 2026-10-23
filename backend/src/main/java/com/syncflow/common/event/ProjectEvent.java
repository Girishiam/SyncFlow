package com.syncflow.common.event;

import java.time.Instant;

/**
 * Integration event published inside a business transaction and delivered to outbound
 * integrations (webhooks) only after that transaction commits.
 *
 * @param payload a serialisable DTO, never a JPA entity
 */
public record ProjectEvent(Long projectId, String type, Object payload, Instant occurredAt) {

    public static final String TASK_CREATED = "TASK_CREATED";
    public static final String TASK_MOVED = "TASK_MOVED";
    public static final String TASK_BLOCKED = "TASK_BLOCKED";
    public static final String SPRINT_COMPLETED = "SPRINT_COMPLETED";

    public static final java.util.Set<String> SUPPORTED = java.util.Set.of(
            TASK_CREATED, TASK_MOVED, TASK_BLOCKED, SPRINT_COMPLETED);

    public static ProjectEvent of(Long projectId, String type, Object payload) {
        return new ProjectEvent(projectId, type, payload, Instant.now());
    }
}
