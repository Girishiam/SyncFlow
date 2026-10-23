package com.syncflow.task.infrastructure.persistence;

import com.syncflow.task.application.port.out.TaskAttachmentRepositoryPort;
import com.syncflow.task.infrastructure.persistence.SpringDataTaskAttachmentRepository;
import com.syncflow.model.TaskAttachment;
import java.util.List;
import java.util.Optional;
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
    public List<TaskAttachment> findByTaskIdOrderByUploadedAtDesc(Long taskId) {
        return repository.findByTaskIdOrderByUploadedAtDesc(taskId);
    }

    @Override
    public void delete(TaskAttachment attachment) {
        repository.delete(attachment);
    }
}
