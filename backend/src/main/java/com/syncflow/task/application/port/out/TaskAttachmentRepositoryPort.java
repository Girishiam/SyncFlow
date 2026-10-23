package com.syncflow.task.application.port.out;

import com.syncflow.model.TaskAttachment;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link TaskAttachment}. */
public interface TaskAttachmentRepositoryPort {
    TaskAttachment save(TaskAttachment attachment);
    Optional<TaskAttachment> findById(Long id);
    List<TaskAttachment> findByTaskIdOrderByUploadedAtDesc(Long taskId);
    void delete(TaskAttachment attachment);
}
