package com.syncflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveTaskRequest {
    @NotNull
    private Long targetStageId;
    private Integer newPosition;
    private Boolean overrideGate;
    private String overrideReason;
    /** Token from POST /v1/auth/elevate with scope WORKFLOW_GATE_OVERRIDE; required when overrideGate is true. */
    private String elevationToken;
}
