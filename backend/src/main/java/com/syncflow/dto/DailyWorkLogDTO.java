package com.syncflow.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyWorkLogDTO {

    private Long id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private String userRole;
    private LocalDate logDate;
    private String summaryText;
    private Integer completedTasksCount;
    private Instant createdAt;
}
