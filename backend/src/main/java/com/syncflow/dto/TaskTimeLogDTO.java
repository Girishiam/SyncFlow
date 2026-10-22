package com.syncflow.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTimeLogDTO {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private Double hoursSpent;
    private String description;
    private LocalDate loggedDate;
    private Instant createdAt;
}
