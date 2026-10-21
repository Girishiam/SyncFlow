package com.syncflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolePermissionsRequest {
    @NotNull
    private Set<Long> permissionIds;
}
