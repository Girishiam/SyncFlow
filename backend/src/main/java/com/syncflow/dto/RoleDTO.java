package com.syncflow.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean isSystemRole;
    private Boolean status;
    private Long assignedUsersCount;
    private List<Long> permissionIds;
    private List<String> permissionAuthorities;
}
