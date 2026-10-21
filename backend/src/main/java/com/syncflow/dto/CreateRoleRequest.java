package com.syncflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {
    @NotBlank
    private String name;
    private String description;
    private Set<Long> permissionIds;
}
