package com.syncflow.task.application.port.in;

import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskDTO;

public interface TaskUseCase {
    TaskDTO createTask(CreateTaskRequest request);
    TaskDTO moveTask(Long id, MoveTaskRequest request);
    TaskDTO updateProgress(Long id, Integer progress);
    TaskDTO setBlocker(Long id, SetBlockerRequest request);
}
