package com.syncflow.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberDTO {
    private UUID userId;
    private String fullName;
    private String email;
    private String avatar;
    /** Organisation-wide role, e.g. DEVELOPER. */
    private String systemRole;
    /** Role in this project: MANAGER, LEAD or MEMBER. */
    private String role;
    private Boolean onLeave;
    /** Story points of unfinished tasks assigned to them in this project. */
    private int openPoints;
    private int openTasks;
    private Instant addedAt;
}
