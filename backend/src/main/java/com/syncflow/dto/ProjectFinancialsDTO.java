package com.syncflow.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectFinancialsDTO {
    private Long projectId;
    private String projectName;
    private String currency;
    private Double totalBudget;
    private Double allocatedBudget;
    private Double spentBudget;
    private Double remainingBudget;
    private Double burnRatePercent;
    private Double estimatedCostPerStoryPoint;
    private Integer totalStoryPoints;
    private Integer completedStoryPoints;
    private Map<String, Double> roleHourlyRates;
    private Map<String, Double> spentByRole;
    private Double projectedCostAtCompletion;
    private Double totalHoursLogged;
    private Double blendedHourlyRate;
}
