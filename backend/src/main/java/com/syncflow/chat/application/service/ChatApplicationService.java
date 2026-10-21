package com.syncflow.chat.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.chat.application.port.in.ChatUseCase;
import com.syncflow.chat.application.port.out.ChannelRepositoryPort;
import com.syncflow.chat.application.port.out.ChatNotificationPort;
import com.syncflow.chat.application.port.out.MessageRepositoryPort;
import com.syncflow.chat.application.port.out.MessageSeenReceiptRepositoryPort;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.model.Channel;
import com.syncflow.model.Message;
import com.syncflow.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatApplicationService implements ChatUseCase {

    private final ChannelRepositoryPort channelRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final MessageSeenReceiptRepositoryPort receiptRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ChatNotificationPort chatNotificationPort;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getAllChannels() {
        return channelRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChannelMessages(Long channelId) {
        List<Message> messages = messageRepositoryPort.findByChannelIdOrderByCreatedAtAsc(channelId);
        List<ChatMessageDTO> dtoList = new ArrayList<>();

        for (Message msg : messages) {
            List<String> seenNames = receiptRepositoryPort.findByMessageId(msg.getId()).stream()
                    .map(r -> r.getUser().getFullName())
                    .collect(Collectors.toList());

            dtoList.add(ChatMessageDTO.builder()
                    .id(msg.getId())
                    .channelId(channelId)
                    .channelName(msg.getChannel().getName())
                    .senderId(msg.getSender().getId())
                    .sender(msg.getSender().getFullName())
                    .text(msg.getContent())
                    .time(TIME_FORMATTER.format(msg.getCreatedAt()))
                    .seenBy(seenNames)
                    .build());
        }

        return dtoList;
    }

    @Override
    @Transactional
    public ChatMessageDTO postMessage(Long channelId, UUID userId, String content) {
        Channel channel = channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new DomainException("Channel not found: " + channelId));

        User sender = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));

        Message message = Message.builder()
                .channel(channel)
                .sender(sender)
                .content(content)
                .isPinned(false)
                .build();

        Message saved = messageRepositoryPort.save(message);

        ChatMessageDTO dto = ChatMessageDTO.builder()
                .id(saved.getId())
                .channelId(channel.getId())
                .channelName(channel.getName())
                .senderId(sender.getId())
                .sender(sender.getFullName())
                .text(saved.getContent())
                .time(TIME_FORMATTER.format(saved.getCreatedAt()))
                .seenBy(List.of("Just now"))
                .build();

        chatNotificationPort.broadcastMessage(channelId, dto);
        return dto;
    }
}
