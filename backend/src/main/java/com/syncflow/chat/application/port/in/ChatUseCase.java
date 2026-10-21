package com.syncflow.chat.application.port.in;

import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.model.Channel;

import java.util.List;
import java.util.UUID;

public interface ChatUseCase {
    List<Channel> getAllChannels();
    List<ChatMessageDTO> getChannelMessages(Long channelId);
    ChatMessageDTO postMessage(Long channelId, UUID userId, String content);
}
