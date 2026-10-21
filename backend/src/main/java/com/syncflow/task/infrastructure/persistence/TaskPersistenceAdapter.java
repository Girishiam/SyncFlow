package com.syncflow.task.infrastructure.persistence;

import com.syncflow.model.Task;
import com.syncflow.repository.TaskRepository;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TaskPersistenceAdapter implements TaskRepositoryPort {

    private final TaskRepository taskRepository;

    @Override
    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    @Override
    public List<Task> findByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    @Override
    public List<Task> findByStageId(Long stageId) {
        return taskRepository.findByStageId(stageId);
    }

    @Override
    public void deleteById(Long id) {
        taskRepository.deleteById(id);
    }
}
