package com.syncflow.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperTimeBreakdownDTO {
    private UUID userId;
    private String fullName;
    private String email;
    private String role;
    private Double totalHours;
    private Integer logsCount;
    private Double percentageOfTotal;
}
