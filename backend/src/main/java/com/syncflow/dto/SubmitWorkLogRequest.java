package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitWorkLogRequest {

    private LocalDate logDate;

    @NotBlank(message = "Summary of work completed today is required")
    private String summaryText;

    private Integer completedTasksCount;
}
