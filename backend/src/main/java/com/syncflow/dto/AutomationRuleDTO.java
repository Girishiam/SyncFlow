package com.syncflow.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutomationRuleDTO {
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String triggerEvent;
    private String actionType;
    private String targetRole;
    private Boolean isEnabled;
    private Integer executionCount;
    private Instant lastExecutedAt;
}
