package com.syncflow.service;

import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.model.Channel;
import com.syncflow.model.Message;
import com.syncflow.model.MessageSeenReceipt;
import com.syncflow.model.User;
import com.syncflow.repository.ChannelRepository;
import com.syncflow.repository.MessageRepository;
import com.syncflow.repository.MessageSeenReceiptRepository;
import com.syncflow.repository.UserRepository;
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
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MessageSeenReceiptRepository receiptRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());

    @Transactional(readOnly = true)
    public List<Channel> getAllChannels() {
        return channelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChannelMessages(Long channelId) {
        List<Message> messages = messageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
        List<ChatMessageDTO> dtoList = new ArrayList<>();

        for (Message msg : messages) {
            List<String> seenNames = receiptRepository.findByMessageId(msg.getId()).stream()
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

    @Transactional
    public ChatMessageDTO postMessage(Long channelId, UUID userId, String content) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Message message = Message.builder()
                .channel(channel)
                .sender(sender)
                .content(content)
                .isPinned(false)
                .build();

        Message saved = messageRepository.save(message);

        return ChatMessageDTO.builder()
                .id(saved.getId())
                .channelId(channel.getId())
                .channelName(channel.getName())
                .senderId(sender.getId())
                .sender(sender.getFullName())
                .text(saved.getContent())
                .time(TIME_FORMATTER.format(saved.getCreatedAt()))
                .seenBy(List.of("Just now"))
                .build();
    }
}
