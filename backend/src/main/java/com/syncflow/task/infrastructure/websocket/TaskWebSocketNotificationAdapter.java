package com.syncflow.task.infrastructure.websocket;

import com.syncflow.common.tx.AfterCommit;
import com.syncflow.dto.TaskDTO;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Pushes task changes to subscribers of the owning project only. Subscriptions to these topics
 * are authorised per project by the STOMP channel interceptor.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskWebSocketNotificationAdapter implements TaskNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;

    public static String boardTopic(Long projectId) {
        return "/topic/projects." + projectId + ".board";
    }

    public static String blockerTopic(Long projectId) {
        return "/topic/projects." + projectId + ".blockers";
    }

    @Override
    public void broadcastBoardUpdate(TaskDTO task) {
        if (task.getProjectId() == null) return;
        AfterCommit.run(() -> messagingTemplate.convertAndSend(boardTopic(task.getProjectId()), task));
    }

    @Override
    public void broadcastBlockerAlert(TaskDTO task) {
        if (task.getProjectId() == null) return;
        AfterCommit.run(() -> messagingTemplate.convertAndSend(blockerTopic(task.getProjectId()), task));
    }
}
