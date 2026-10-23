package com.syncflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;
    private LocalDate dueDate;
    @jakarta.validation.constraints.PositiveOrZero
    private Double totalBudget;
    @jakarta.validation.constraints.PositiveOrZero
    private Double allocatedBudget;
    @Size(max = 10)
    private String currency;

    /** The initial team. The creator is always added as MANAGER. */
    @Valid
    @Builder.Default
    private List<ProjectMemberRequest> members = new ArrayList<>();
}
