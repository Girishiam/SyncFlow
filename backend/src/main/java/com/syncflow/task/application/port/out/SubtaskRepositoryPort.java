package com.syncflow.task.application.port.out;

import com.syncflow.model.Subtask;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Subtask}. */
public interface SubtaskRepositoryPort {
    Subtask save(Subtask subtask);
    Optional<Subtask> findById(Long id);
    List<Subtask> findByTaskIdOrderByPositionAsc(Long taskId);
    long countByTaskId(Long taskId);
    long countByTaskIdAndIsCompletedTrue(Long taskId);
    void delete(Subtask subtask);
    void flush();
}
