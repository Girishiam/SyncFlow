package com.syncflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetBlockerRequest {
    @NotNull
    private Boolean isBlocked;
    private String blockerReason;
}
