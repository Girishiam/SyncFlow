package com.syncflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProgressRequest {
    @NotNull
    @Min(0)
    @Max(100)
    private Integer progress;
}
