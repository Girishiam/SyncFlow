package com.syncflow.task.application.port.out;

import com.syncflow.model.TaskComment;
import java.util.List;

public interface TaskCommentRepositoryPort {
    List<TaskComment> findByTaskId(Long taskId);
    TaskComment save(TaskComment comment);
}
