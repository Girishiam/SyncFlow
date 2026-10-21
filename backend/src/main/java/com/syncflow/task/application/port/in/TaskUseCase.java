package com.syncflow.task.application.port.in;

import com.syncflow.dto.AddCommentRequest;
import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskCommentDTO;
import com.syncflow.dto.TaskDTO;

import java.util.List;
import java.util.UUID;

public interface TaskUseCase {
    TaskDTO createTask(CreateTaskRequest request);
    TaskDTO moveTask(Long id, MoveTaskRequest request);
    TaskDTO updateProgress(Long id, Integer progress);
    TaskDTO setBlocker(Long id, SetBlockerRequest request);
    TaskDTO getTaskDetails(Long id);
    List<TaskCommentDTO> getTaskComments(Long taskId);
    TaskCommentDTO addComment(Long taskId, AddCommentRequest request, UUID authorId);
}
