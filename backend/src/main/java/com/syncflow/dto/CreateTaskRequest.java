package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {
    @NotNull
    private Long projectId;

    @NotNull
    private Long stageId;

    @NotBlank
    private String title;

    private String description;

    @Builder.Default
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    private UUID assigneeId;

    private java.time.Instant deadline;
}
