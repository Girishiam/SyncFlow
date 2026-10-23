package com.syncflow.chat.application.port.in;

import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.CreateChannelRequest;
import com.syncflow.dto.UpdateChannelRequest;
import com.syncflow.dto.UserDTO;
import com.syncflow.model.Channel;

import java.util.List;
import java.util.UUID;

/** Chat use cases; the acting user is always the authenticated principal. */
public interface ChatUseCase {
    /** Public channels plus private channels the current user belongs to. */
    List<Channel> getAllChannels();

    /** Up to {@code limit} messages older than {@code beforeId} (or the newest), oldest first. */
    List<ChatMessageDTO> getChannelMessages(Long channelId, Long beforeId, int limit);

    ChatMessageDTO postMessage(Long channelId, String content);
    Channel createChannel(CreateChannelRequest request);
    Channel updateChannel(Long channelId, UpdateChannelRequest request);
    void deleteChannel(Long channelId);
    List<UserDTO> getChannelMembers(Long channelId);
    void addMemberToChannel(Long channelId, UUID memberId);
    void removeMemberFromChannel(Long channelId, UUID memberId);
}
