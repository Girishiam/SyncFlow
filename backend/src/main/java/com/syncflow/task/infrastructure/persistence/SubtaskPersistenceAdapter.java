package com.syncflow.task.infrastructure.persistence;

import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.infrastructure.persistence.SpringDataSubtaskRepository;
import com.syncflow.model.Subtask;
import java.util.*;
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
    public Map<Long, long[]> checklistCounts(Collection<Long> taskIds) {
        Map<Long, long[]> counts = new HashMap<>();
        for (List<Long> chunk : chunks(taskIds)) {
            for (Object[] row : repository.countByTaskIds(chunk)) {
                counts.put((Long) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
            }
        }
        return counts;
    }

    /** SQL Server caps a statement at 2100 parameters, so large id sets are queried in chunks. */
    private static <T> List<List<T>> chunks(Collection<T> items) {
        List<T> all = new ArrayList<>(items);
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i += 1000) out.add(all.subList(i, Math.min(all.size(), i + 1000)));
        return out;
    }

    @Override
    public void flush() {
        repository.flush();
    }
}
