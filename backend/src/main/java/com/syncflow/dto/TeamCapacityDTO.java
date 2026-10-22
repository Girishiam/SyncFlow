package com.syncflow.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamCapacityDTO {
    private Long projectId;
    private String projectName;
    private Integer totalSprintCapacity;
    private Integer totalAssignedPoints;
    private Double overallUtilizationPercent;
    private Integer overloadedMembersCount;
    private List<MemberCapacityDTO> members;
}
