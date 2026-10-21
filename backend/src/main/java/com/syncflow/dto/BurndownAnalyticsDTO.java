package com.syncflow.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BurndownAnalyticsDTO {
    private Long projectId;
    private String projectName;
    private Integer sprintDurationDays;
    private Integer currentDay;
    private Integer totalStoryPoints;
    private Integer remainingStoryPoints;
    private Double completionPercentage;
    private Integer velocityPointsPerWeek;
    private Double averageCycleTimeDays;
    private Integer daysRemainingInSprint;
    private List<BurndownDayDTO> dailyProgress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BurndownDayDTO {
        private Integer day;
        private String label;
        private Double idealRemaining;
        private Double actualRemaining;
        private Integer completedTasks;
    }
}
