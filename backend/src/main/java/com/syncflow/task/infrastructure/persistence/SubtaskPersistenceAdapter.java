package com.syncflow.task.infrastructure.persistence;

import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.infrastructure.persistence.SpringDataSubtaskRepository;
import com.syncflow.model.Subtask;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubtaskPersistenceAdapter implements SubtaskRepositoryPort {

    private final SpringDataSubtaskRepository repository;

    @Override
    public Subtask save(Subtask subtask) {
        return repository.save(subtask);
    }

    @Override
    public Optional<Subtask> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Subtask> findByTaskIdOrderByPositionAsc(Long taskId) {
        return repository.findByTaskIdOrderByPositionAsc(taskId);
    }

    @Override
    public long countByTaskId(Long taskId) {
        return repository.countByTaskId(taskId);
    }

    @Override
    public long countByTaskIdAndIsCompletedTrue(Long taskId) {
        return repository.countByTaskIdAndIsCompletedTrue(taskId);
    }

    @Override
    public void delete(Subtask subtask) {
        repository.delete(subtask);
    }

    @Override
    public void flush() {
        repository.flush();
    }
}
