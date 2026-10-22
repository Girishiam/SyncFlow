package com.syncflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "secret_token", length = 200)
    private String secretToken;

    @Column(name = "events", length = 500)
    @Builder.Default
    private String events = "TASK_CREATED,TASK_MOVED,TASK_BLOCKED,SPRINT_COMPLETED";

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "delivery_count")
    @Builder.Default
    private Integer deliveryCount = 0;

    @Column(name = "last_delivered_at")
    private Instant lastDeliveredAt;

    @Column(name = "last_response_status", length = 50)
    private String lastResponseStatus;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
