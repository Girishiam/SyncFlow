package com.syncflow.notification.infrastructure.websocket;

import com.syncflow.dto.NotificationDTO;
import com.syncflow.notification.application.port.out.NotificationWebSocketPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketAdapter implements NotificationWebSocketPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void pushToUser(String userId, NotificationDTO notification) {
        log.debug("Pushing notification {} to /topic/notifications.{}", notification.getId(), userId);
        messagingTemplate.convertAndSend("/topic/notifications." + userId, notification);
    }

    @Override
    public void pushToRole(String roleName, NotificationDTO notification) {
        log.debug("Pushing notification {} to role /topic/notifications.role.{}", notification.getId(), roleName);
        messagingTemplate.convertAndSend("/topic/notifications.role." + roleName, notification);
    }

    @Override
    public void broadcast(NotificationDTO notification) {
        log.debug("Broadcasting notification {} to /topic/notifications", notification.getId());
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
