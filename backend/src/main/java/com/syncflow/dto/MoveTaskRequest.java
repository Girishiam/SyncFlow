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
}
