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
}
