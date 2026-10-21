package com.syncflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Specific recipient (null if targeted to role or system-wide)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    // Role-based target (e.g. TEAM_LEAD, ADMIN, SQA)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_role_id")
    private Role targetRole;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    // TASK_ASSIGNED, TASK_DUE_SOON, TASK_OVERDUE, CHANNEL_MENTION, BLOCKER_ALERT, ROLE_UPDATE, SYSTEM
    @Column(nullable = false, length = 50)
    private String type;

    // INFO, WARNING, CRITICAL
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "INFO";

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    // NAVIGATE_TASK, NAVIGATE_CHANNEL, NAVIGATE_RBAC, NONE
    @Column(name = "action_type", length = 50)
    @Builder.Default
    private String actionType = "NONE";

    // Task ID or Channel ID
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "sender_avatar", length = 20)
    private String senderAvatar;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;
}
