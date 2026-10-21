package com.syncflow.project.application.port.out;

import com.syncflow.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectRepositoryPort {
    List<Project> findAll();
    Optional<Project> findById(Long id);
    Project save(Project project);
}
