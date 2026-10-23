package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStageRequest {
    @NotBlank(message = "Stage name is required")
    private String name;
    private Integer position;
    private Boolean isDoneStage;
    /** Gate flags; when omitted a done stage requires a deliverable and no blockers. */
    private Boolean requiresChecklistComplete;
    private Boolean requiresDeliverable;
    private Boolean requiresNoBlockers;
}
