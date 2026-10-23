package com.syncflow.task.infrastructure.persistence;

import com.syncflow.task.application.port.out.TaskTimeLogRepositoryPort;
import com.syncflow.repository.TaskTimeLogRepository;
import com.syncflow.model.TaskTimeLog;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskTimeLogPersistenceAdapter implements TaskTimeLogRepositoryPort {

    private final TaskTimeLogRepository repository;

    @Override
    public TaskTimeLog save(TaskTimeLog log) {
        return repository.save(log);
    }

    @Override
    public Optional<TaskTimeLog> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<TaskTimeLog> findByTaskIdOrderByCreatedAtDesc(Long taskId) {
        return repository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Override
    public List<TaskTimeLog> findByTaskProjectIdOrderByCreatedAtDesc(Long projectId) {
        return repository.findByTaskProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Override
    public Double sumHoursByTaskId(Long taskId) {
        return repository.sumHoursByTaskId(taskId);
    }

    @Override
    public void delete(TaskTimeLog log) {
        repository.delete(log);
    }

    @Override
    public void flush() {
        repository.flush();
    }
}
