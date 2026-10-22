package com.syncflow.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberCapacityDTO {
    private String userId;
    private String fullName;
    private String role;
    private String avatar;
    private Integer maxCapacityStoryPoints;
    private Integer assignedStoryPoints;
    private Double utilizationPercent;
    private Integer assignedTaskCount;
    private Boolean isOverloaded;
    private Boolean onLeave;
    private String status; // "OPTIMAL", "OVERLOADED", "UNDERUTILIZED", "ON_LEAVE"
    private List<String> assignedTaskTitles;
}
