package com.syncflow.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTimeSummaryDTO {
    private Long projectId;
    private String projectName;
    private Double totalHoursSpent;
    private Double totalEstimatedHours;
    private Double hoursRemaining;
    private Double progressPercentage;
    private Integer totalLogsCount;
    private Integer tasksWithLogsCount;
    private List<DeveloperTimeBreakdownDTO> developerBreakdown;
    private List<StageTimeBreakdownDTO> stageBreakdown;
    private List<TaskTimeLogDTO> recentLogs;
}
