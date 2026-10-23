package com.syncflow.task.application.port.out;

import com.syncflow.model.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryPort {
    Task save(Task task);
    Optional<Task> findById(Long id);
    List<Task> findByProjectId(Long projectId);
    /** Non-archived tasks with stage and assignee loaded (board views). */
    List<Task> findByProjectIdAndIsArchivedFalse(Long projectId);
    List<Task> findByProjectIdAndIsArchivedTrue(Long projectId);
    List<Task> findBySprintId(Long sprintId);
    List<Task> findAllById(java.util.Collection<Long> ids);
    List<Task> findByStageId(Long stageId);
    List<Task> findByAssigneeId(java.util.UUID assigneeId);
    List<Task> findAll();
    List<Task> findOpenAssignedWithDeadlineBefore(java.time.Instant until);
    void deleteById(Long id);
}
