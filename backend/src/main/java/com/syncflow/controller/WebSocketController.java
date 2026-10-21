package com.syncflow.controller;

import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.TaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/board.move")
    @SendTo("/topic/board")
    public TaskDTO handleTaskMove(@Payload TaskDTO task) {
        log.info("WebSocket: Broadcasting Task Move for Task ID: {}", task.getId());
        return task;
    }

    @MessageMapping("/chat.send.{channelId}")
    public void handleChatMessage(@DestinationVariable Long channelId, @Payload ChatMessageDTO message) {
        log.info("WebSocket: Broadcasting chat message to channel {}", channelId);
        messagingTemplate.convertAndSend("/topic/channel." + channelId, message);
    }
}
