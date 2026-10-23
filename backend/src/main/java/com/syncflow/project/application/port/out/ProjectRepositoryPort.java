package com.syncflow.project.application.port.out;

import com.syncflow.model.User;
import java.util.UUID;

import com.syncflow.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepositoryPort {
    List<Project> findAll();
    Optional<Project> findById(Long id);
    Project save(Project project);

    /** Whether the user may access the project (organisation-wide or team member/lead/manager). */
    boolean isAccessibleBy(Long projectId, UUID userId);

    List<Project> findAccessibleBy(UUID userId);

    /** Team members, team lead and project manager. */
    List<User> findTeamMembers(Long projectId);
}
