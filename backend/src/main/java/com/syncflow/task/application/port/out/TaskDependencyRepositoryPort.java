package com.syncflow.task.application.port.out;

import com.syncflow.model.TaskDependency;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link TaskDependency}. */
public interface TaskDependencyRepositoryPort {
    TaskDependency save(TaskDependency dependency);
    Optional<TaskDependency> findById(Long id);
    List<TaskDependency> findBySourceTaskId(Long sourceTaskId);
    List<TaskDependency> findByTargetTaskId(Long targetTaskId);
    List<TaskDependency> findBySourceTaskIdIn(Collection<Long> sourceTaskIds);
    List<TaskDependency> findByTargetTaskIdIn(Collection<Long> targetTaskIds);
    boolean existsBySourceTaskIdAndTargetTaskId(Long sourceTaskId, Long targetTaskId);
    void delete(TaskDependency dependency);
}
