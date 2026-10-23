package com.syncflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private UUID recipientId;
    private String recipientName;
    private String title;
    private String message;
    private String type; // TASK_ASSIGNED, TASK_DUE_SOON, TASK_OVERDUE, CHANNEL_MENTION, BLOCKER_ALERT, ROLE_UPDATE, SYSTEM
    private String priority; // INFO, WARNING, CRITICAL
    private Boolean isRead;
    private String linkUrl;
    private String actionType; // NAVIGATE_TASK, NAVIGATE_CHANNEL, NAVIGATE_RBAC, NONE
    private Long referenceId;
    private String senderName;
    private String senderAvatar;
    private Instant createdAt;
    private Instant readAt;
    private String timeAgo;
}
