package com.syncflow.chat.infrastructure.websocket;

import com.syncflow.chat.application.port.out.ChatNotificationPort;
import com.syncflow.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketNotificationAdapter implements ChatNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastMessage(Long channelId, ChatMessageDTO message) {
        log.debug("Broadcasting chat message to /topic/channel.{}", channelId);
        messagingTemplate.convertAndSend("/topic/channel." + channelId, message);
    }
}
