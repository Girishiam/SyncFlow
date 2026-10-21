package com.syncflow.dto;

import com.syncflow.model.Role;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String status;
    private Boolean onLeave;
    private String leaveNote;
    private Role role;
    private Set<String> permissions;
}
