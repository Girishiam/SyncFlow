package com.syncflow.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SprintDTO {
    private Long id;
    private Long projectId;
    private String name;
    private String goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // PLANNING, ACTIVE, COMPLETED
    private Integer targetStoryPoints;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer totalStoryPoints;
    private Integer completedStoryPoints;
}
