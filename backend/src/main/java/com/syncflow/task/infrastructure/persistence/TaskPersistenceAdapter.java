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
    public List<Task> findByProjectIdAndIsArchivedFalse(Long projectId) {
        return taskRepository.findByProjectIdAndIsArchivedFalse(projectId);
    }

    @Override
    public List<Task> findByProjectIdAndIsArchivedTrue(Long projectId) {
        return taskRepository.findByProjectIdAndIsArchivedTrue(projectId);
    }

    @Override
    public List<Task> findBySprintId(Long sprintId) {
        return taskRepository.findBySprintId(sprintId);
    }

    @Override
    public List<Task> findAllById(java.util.Collection<Long> ids) {
        return taskRepository.findAllById(ids);
    }

    @Override
    public List<Task> findByStageId(Long stageId) {
        return taskRepository.findByStageId(stageId);
    }

    @Override
    public List<Task> findByAssigneeId(java.util.UUID assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId);
    }

    @Override
    public List<Task> findAll() {
        return taskRepository.findAll();
    }

    @Override
    public List<Task> findOpenAssignedWithDeadlineBefore(java.time.Instant until) {
        return taskRepository.findOpenAssignedWithDeadlineBefore(until);
    }

    @Override
    public void deleteById(Long id) {
        taskRepository.deleteById(id);
    }
}
