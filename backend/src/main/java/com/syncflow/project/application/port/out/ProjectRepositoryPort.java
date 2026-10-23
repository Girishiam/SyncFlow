package com.syncflow.project.application.port.out;

import com.syncflow.model.ProjectMember;
import com.syncflow.model.User;
import java.util.UUID;

import com.syncflow.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepositoryPort {
    List<Project> findAll();
    Optional<Project> findById(Long id);
    Project save(Project project);

    /** Whether the user is on the project team (a member or the project manager). */
    boolean isAccessibleBy(Long projectId, UUID userId);

    List<Project> findAccessibleBy(UUID userId);

    /** Every member plus the project manager. */
    List<User> findTeamMembers(Long projectId);

    // --- membership
    List<ProjectMember> findMembers(Long projectId);
    Optional<ProjectMember> findMember(Long projectId, UUID userId);
    ProjectMember saveMember(ProjectMember member);
    void deleteMember(ProjectMember member);
    long countMembers(Long projectId);
}
