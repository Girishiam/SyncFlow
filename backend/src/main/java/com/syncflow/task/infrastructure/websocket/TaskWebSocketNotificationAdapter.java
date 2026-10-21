package com.syncflow.task.infrastructure.websocket;

import com.syncflow.dto.TaskDTO;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskWebSocketNotificationAdapter implements TaskNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastBoardUpdate(TaskDTO task) {
        log.debug("Broadcasting board update for task ID {}", task.getId());
        messagingTemplate.convertAndSend("/topic/board", task);
    }

    @Override
    public void broadcastBlockerAlert(TaskDTO task) {
        log.warn("Broadcasting blocker alert for task ID {}: {}", task.getId(), task.getBlockerReason());
        messagingTemplate.convertAndSend("/topic/blocker-alerts", task);
    }
}
