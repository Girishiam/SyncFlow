package com.syncflow.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowGateValidationResultDTO {
    private Boolean allowed;
    private String targetStageName;
    private Boolean requiresOverride;
    private List<String> violations;
    private List<String> blockingTaskTitles;
    private Integer uncompletedSubtasksCount;
    private Boolean hasDeliverableAttachment;
}
