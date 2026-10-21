package com.syncflow.task.application.port.out;

import com.syncflow.dto.TaskDTO;

public interface TaskNotificationPort {
    void broadcastBoardUpdate(TaskDTO task);
    void broadcastBlockerAlert(TaskDTO task);
}
