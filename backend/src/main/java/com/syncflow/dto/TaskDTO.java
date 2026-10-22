package com.syncflow.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private Integer progress;
    private Long stageId;
    private String stageName;
    private Long projectId;
    private String projectName;
    private UUID assigneeId;
    private String assignee;
    private String avatar;
    private Boolean isBlocked;
    private String blockerReason;
    private java.time.Instant deadline;
    private Boolean isArchived;
    private java.time.Instant archivedAt;
    private String archivedBy;
    private java.time.Instant startDate;
    private Long sprintId;
    private String sprintName;
    private Integer storyPoints;
    private String gitBranch;
    private String gitCommitHash;
    private String gitPrUrl;
}
