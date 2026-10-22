package com.syncflow.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageTimeBreakdownDTO {
    private Long stageId;
    private String stageName;
    private Double totalHours;
    private Integer tasksCount;
    private Double percentageOfTotal;
}
