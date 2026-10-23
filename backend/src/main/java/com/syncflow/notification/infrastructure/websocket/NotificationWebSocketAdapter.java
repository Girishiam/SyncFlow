package com.syncflow.notification.infrastructure.websocket;

import com.syncflow.common.tx.AfterCommit;
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

    /** Clients subscribe to /user/queue/notifications; only the authenticated owner receives these frames. */
    public static final String USER_QUEUE = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void pushToUser(String principalName, NotificationDTO notification) {
        AfterCommit.run(() -> {
            log.debug("Pushing notification {} to {}", notification.getId(), principalName);
            messagingTemplate.convertAndSendToUser(principalName, USER_QUEUE, notification);
        });
    }

}
