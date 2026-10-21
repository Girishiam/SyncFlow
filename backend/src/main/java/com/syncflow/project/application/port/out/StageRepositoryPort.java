package com.syncflow.project.application.port.out;

import com.syncflow.model.Stage;
import java.util.List;
import java.util.Optional;

public interface StageRepositoryPort {
    List<Stage> findByProjectIdOrderByPositionAsc(Long projectId);
    Optional<Stage> findById(Long id);
    Stage save(Stage stage);
}
