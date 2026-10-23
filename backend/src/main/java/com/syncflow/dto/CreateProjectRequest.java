package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
    @jakarta.validation.constraints.PositiveOrZero
    private Double totalBudget;
    @jakarta.validation.constraints.PositiveOrZero
    private Double allocatedBudget;
    @jakarta.validation.constraints.Size(max = 10)
    private String currency;
}
