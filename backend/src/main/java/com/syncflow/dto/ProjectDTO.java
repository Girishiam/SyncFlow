package com.syncflow.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {
    private Long id;
    private String name;
    private String description;
    private String status;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String managerName;
    private long memberCount;
    /** The caller's role in this project (MANAGER, LEAD, MEMBER), or null when they only see it as an admin. */
    private String myRole;
    /** Whether the caller can add, remove or change members. */
    private boolean canManageTeam;
}
