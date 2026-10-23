package com.syncflow.task.infrastructure.persistence;

import com.syncflow.task.application.port.out.TaskDependencyRepositoryPort;
import com.syncflow.repository.TaskDependencyRepository;
import com.syncflow.model.TaskDependency;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskDependencyPersistenceAdapter implements TaskDependencyRepositoryPort {

    private final TaskDependencyRepository repository;

    @Override
    public TaskDependency save(TaskDependency dependency) {
        return repository.save(dependency);
    }

    @Override
    public Optional<TaskDependency> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<TaskDependency> findBySourceTaskId(Long sourceTaskId) {
        return repository.findBySourceTaskId(sourceTaskId);
    }

    @Override
    public List<TaskDependency> findByTargetTaskId(Long targetTaskId) {
        return repository.findByTargetTaskId(targetTaskId);
    }

    @Override
    public List<TaskDependency> findBySourceTaskIdIn(Collection<Long> sourceTaskIds) {
        return repository.findBySourceTaskIdIn(sourceTaskIds);
    }

    @Override
    public List<TaskDependency> findByTargetTaskIdIn(Collection<Long> targetTaskIds) {
        return repository.findByTargetTaskIdIn(targetTaskIds);
    }

    @Override
    public boolean existsBySourceTaskIdAndTargetTaskId(Long sourceTaskId, Long targetTaskId) {
        return repository.existsBySourceTaskIdAndTargetTaskId(sourceTaskId, targetTaskId);
    }

    @Override
    public void delete(TaskDependency dependency) {
        repository.delete(dependency);
    }
}
