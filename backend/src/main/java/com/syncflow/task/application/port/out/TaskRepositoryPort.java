package com.syncflow.task.application.port.out;

import com.syncflow.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryPort {
    Task save(Task task);
    Optional<Task> findById(Long id);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByStageId(Long stageId);
    void deleteById(Long id);
}
