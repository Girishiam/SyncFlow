package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Re-authentication request used to unlock a privileged action for a few minutes. */
@Data
@NoArgsConstructor
public class ElevationRequest {
    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Scope is required")
    private String scope;
}
