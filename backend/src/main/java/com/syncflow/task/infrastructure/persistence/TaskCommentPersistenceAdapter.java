package com.syncflow.task.infrastructure.persistence;

import com.syncflow.model.TaskComment;
import com.syncflow.repository.TaskCommentRepository;
import com.syncflow.task.application.port.out.TaskCommentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskCommentPersistenceAdapter implements TaskCommentRepositoryPort {

    private final TaskCommentRepository commentRepository;

    @Override
    public List<TaskComment> findByTaskId(Long taskId) {
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @Override
    public TaskComment save(TaskComment comment) {
        return commentRepository.save(comment);
    }
}
