package com.syncflow.chat.application.port.in;

import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.CreateChannelRequest;
import com.syncflow.dto.UpdateChannelRequest;
import com.syncflow.dto.UserDTO;
import com.syncflow.model.Channel;

import java.util.List;
import java.util.UUID;

public interface ChatUseCase {
    List<Channel> getAllChannels();
    List<ChatMessageDTO> getChannelMessages(Long channelId);
    ChatMessageDTO postMessage(Long channelId, UUID userId, String content);

    Channel createChannel(CreateChannelRequest request, UUID creatorId);
    Channel updateChannel(Long channelId, UpdateChannelRequest request, UUID userId);
    void deleteChannel(Long channelId, UUID userId);

    List<UserDTO> getChannelMembers(Long channelId);
    void addMemberToChannel(Long channelId, UUID memberId, UUID requesterId);
    void removeMemberFromChannel(Long channelId, UUID memberId, UUID requesterId);
}

