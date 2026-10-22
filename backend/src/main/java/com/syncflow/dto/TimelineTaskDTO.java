package com.syncflow.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineTaskDTO {
    private Long id;
    private String title;
    private String priority;
    private Integer progress;
    private Long stageId;
    private String stageName;
    private Boolean isDoneStage;
    private String assignee;
    private String avatar;
    private Instant startDate;
    private Instant deadline;
    private Boolean isBlocked;
    private Integer storyPoints;
    private List<Long> blockedByTaskIds;
}
