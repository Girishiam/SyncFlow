package com.syncflow.task.application.port.out;

import com.syncflow.model.TaskTimeLog;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link TaskTimeLog}. */
public interface TaskTimeLogRepositoryPort {
    TaskTimeLog save(TaskTimeLog log);
    Optional<TaskTimeLog> findById(Long id);
    List<TaskTimeLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<TaskTimeLog> findByTaskProjectIdOrderByCreatedAtDesc(Long projectId);
    Double sumHoursByTaskId(Long taskId);
    void delete(TaskTimeLog log);
    void flush();
}
