package com.syncflow.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String actorName;
    private String actorEmail;
    private String actorRole;
    private String actorAvatar;
    private String details;
    private String ipAddress;
    private String verificationHash;
    private Instant timestamp;
    private String formattedDate;
    private String timeAgo;
}
