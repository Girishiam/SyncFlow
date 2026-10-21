package com.syncflow.chat.application.port.out;

import com.syncflow.dto.ChatMessageDTO;

public interface ChatNotificationPort {
    void broadcastMessage(Long channelId, ChatMessageDTO message);
}
