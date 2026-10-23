package com.syncflow.model;

import com.syncflow.model.enums.*;

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

    /** Every notification belongs to exactly one user (role-wide alerts are fanned out per user). */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    // TASK_ASSIGNED, TASK_DUE_SOON, TASK_OVERDUE, CHANNEL_MENTION, BLOCKER_ALERT, ROLE_UPDATE, SYSTEM
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // INFO, WARNING, CRITICAL
    @Column(nullable = false, length = 20)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private NotificationPriority priority = NotificationPriority.INFO;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    // NAVIGATE_TASK, NAVIGATE_CHANNEL, NAVIGATE_RBAC, NONE
    @Column(name = "action_type", length = 50)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private NotificationAction actionType = NotificationAction.NONE;

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
