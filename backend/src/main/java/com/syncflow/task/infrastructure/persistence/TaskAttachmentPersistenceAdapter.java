package com.syncflow.task.infrastructure.persistence;

import com.syncflow.task.application.port.out.TaskAttachmentRepositoryPort;
import com.syncflow.task.infrastructure.persistence.SpringDataTaskAttachmentRepository;
import com.syncflow.model.TaskAttachment;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskAttachmentPersistenceAdapter implements TaskAttachmentRepositoryPort {

    private final SpringDataTaskAttachmentRepository repository;

    @Override
    public TaskAttachment save(TaskAttachment attachment) {
        return repository.save(attachment);
    }

    @Override
    public Optional<TaskAttachment> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Map<Long, Long> attachmentCounts(Collection<Long> taskIds) {
        Map<Long, Long> counts = new HashMap<>();
        for (List<Long> chunk : chunks(taskIds)) {
            for (Object[] row : repository.countByTaskIds(chunk)) {
                counts.put((Long) row[0], ((Number) row[1]).longValue());
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
    public List<TaskAttachment> findByTaskIdOrderByUploadedAtDesc(Long taskId) {
        return repository.findByTaskIdOrderByUploadedAtDesc(taskId);
    }

    @Override
    public void delete(TaskAttachment attachment) {
        repository.delete(attachment);
    }
}
