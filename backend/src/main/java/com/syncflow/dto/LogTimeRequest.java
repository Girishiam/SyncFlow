package com.syncflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogTimeRequest {

    @NotNull(message = "Hours spent is required")
    @DecimalMin(value = "0.05", message = "Must log at least 0.05 hours (3 minutes)")
    private Double hoursSpent;

    private String description;

    private LocalDate loggedDate;

    private Double estimatedHours;
}
