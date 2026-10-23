package com.syncflow.dto;

import lombok.*;

import java.util.UUID;

/** Adds a user to a project or changes their project role. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberRequest {
    private UUID userId;
    /** MANAGER, LEAD or MEMBER. Defaults to MEMBER. */
    private String role;
}
