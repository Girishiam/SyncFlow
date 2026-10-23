package com.syncflow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Partial update of a stage; null fields are left unchanged. */
@Data
@NoArgsConstructor
public class UpdateStageRequest {
    @Size(min = 1, max = 80)
    private String name;
    private Boolean isDoneStage;
    private Boolean requiresChecklistComplete;
    private Boolean requiresDeliverable;
    private Boolean requiresNoBlockers;
}
