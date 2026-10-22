package com.syncflow.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependencyDTO {
    private Long id;
    private Long sourceTaskId;
    private Long targetTaskId;
    private String targetTaskTitle;
    private String targetTaskPriority;
    private Integer targetTaskProgress;
    private String targetTaskStageName;
    private String targetTaskAssignee;
    private String dependencyType; // BLOCKS, BLOCKED_BY, RELATES_TO
    private Boolean isBlocked;
    private String createdBy;
    private Instant createdAt;
}
