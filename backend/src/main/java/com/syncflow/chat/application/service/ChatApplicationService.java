package com.syncflow.chat.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.chat.application.port.in.ChatUseCase;
import com.syncflow.chat.application.port.out.ChannelRepositoryPort;
import com.syncflow.chat.application.port.out.ChatNotificationPort;
import com.syncflow.chat.application.port.out.MessageRepositoryPort;
import com.syncflow.chat.application.port.out.MessageSeenReceiptRepositoryPort;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.CreateChannelRequest;
import com.syncflow.dto.UpdateChannelRequest;
import com.syncflow.dto.UserDTO;
import com.syncflow.model.Channel;
import com.syncflow.model.Message;
import com.syncflow.model.Project;
import com.syncflow.model.User;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatApplicationService implements ChatUseCase {

    private final ChannelRepositoryPort channelRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final MessageSeenReceiptRepositoryPort receiptRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProjectRepositoryPort projectRepositoryPort;
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

    @Override
    @Transactional
    public Channel createChannel(CreateChannelRequest request, UUID creatorId) {
        String rawName = request.getName() != null ? request.getName() : "new-channel";
        String cleanName = rawName.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        if (cleanName.isBlank()) {
            cleanName = "channel-" + System.currentTimeMillis() % 10000;
        }

        if (channelRepositoryPort.existsByName(cleanName)) {
            throw new DomainException("Channel with name #" + cleanName + " already exists");
        }

        User creator = null;
        if (creatorId != null) {
            creator = userRepositoryPort.findById(creatorId).orElse(null);
        }

        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepositoryPort.findById(request.getProjectId()).orElse(null);
        }

        Set<User> members = new HashSet<>();
        if (creator != null) {
            members.add(creator);
        }

        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            for (UUID mId : request.getMemberIds()) {
                userRepositoryPort.findById(mId).ifPresent(members::add);
            }
        }

        Channel channel = Channel.builder()
                .name(cleanName)
                .description(request.getDescription())
                .type(request.getType() != null && !request.getType().isBlank() ? request.getType() : "TEXT")
                .icon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon() : "hash")
                .isPrivate(Boolean.TRUE.equals(request.getIsPrivate()))
                .project(project)
                .createdBy(creator)
                .members(members)
                .build();

        Channel saved = channelRepositoryPort.save(channel);
        log.info("Created new enterprise channel: #{} (id: {}) by user: {}", saved.getName(), saved.getId(), creatorId);
        return saved;
    }

    @Override
    @Transactional
    public Channel updateChannel(Long channelId, UpdateChannelRequest request, UUID userId) {
        Channel channel = channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new DomainException("Channel not found: " + channelId));

        if (request.getName() != null && !request.getName().isBlank()) {
            String cleanName = request.getName().trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
            if (!channel.getName().equalsIgnoreCase(cleanName)) {
                if ("general".equalsIgnoreCase(channel.getName())) {
                    throw new DomainException("The default #general channel name cannot be modified.");
                }
                if (channelRepositoryPort.existsByName(cleanName)) {
                    throw new DomainException("Channel with name #" + cleanName + " already exists");
                }
                channel.setName(cleanName);
            }
        }

        if (request.getDescription() != null) {
            channel.setDescription(request.getDescription());
        }
        if (request.getType() != null && !request.getType().isBlank()) {
            channel.setType(request.getType());
        }
        if (request.getIcon() != null && !request.getIcon().isBlank()) {
            channel.setIcon(request.getIcon());
        }
        if (request.getIsPrivate() != null) {
            channel.setIsPrivate(request.getIsPrivate());
        }

        Channel updated = channelRepositoryPort.save(channel);
        log.info("Updated channel #{} (id: {}) by user: {}", updated.getName(), updated.getId(), userId);
        return updated;
    }

    @Override
    @Transactional
    public void deleteChannel(Long channelId, UUID userId) {
        Channel channel = channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new DomainException("Channel not found: " + channelId));

        if ("general".equalsIgnoreCase(channel.getName())) {
            throw new DomainException("The default #general channel cannot be deleted.");
        }

        // Clean up receipts and messages associated with this channel
        receiptRepositoryPort.deleteByChannelId(channelId);
        messageRepositoryPort.deleteByChannelId(channelId);

        channel.getMembers().clear();
        channelRepositoryPort.deleteById(channelId);
        log.info("Deleted channel #{} (id: {}) by user: {}", channel.getName(), channelId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getChannelMembers(Long channelId) {
        Channel channel = channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new DomainException("Channel not found: " + channelId));

        return channel.getMembers().stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addMemberToChannel(Long channelId, UUID memberId, UUID requesterId) {
        Channel channel = channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new DomainException("Channel not found: " + channelId));

        User member = userRepositoryPort.findById(memberId)
                .orElseThrow(() -> new DomainException("User not found: " + memberId));

        channel.getMembers().add(member);
        channelRepositoryPort.save(channel);
        log.info("Added member {} to channel #{} by requester {}", member.getFullName(), channel.getName(), requesterId);
    }

    @Override
    @Transactional
    public void removeMemberFromChannel(Long channelId, UUID memberId, UUID requesterId) {
        Channel channel = channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new DomainException("Channel not found: " + channelId));

        User member = userRepositoryPort.findById(memberId)
                .orElseThrow(() -> new DomainException("User not found: " + memberId));

        channel.getMembers().remove(member);
        channelRepositoryPort.save(channel);
        log.info("Removed member {} from channel #{} by requester {}", member.getFullName(), channel.getName(), requesterId);
    }

    private UserDTO toUserDTO(User user) {
        if (user == null) return null;
        Set<String> perms = user.getRole() != null && user.getRole().getPermissions() != null
                ? user.getRole().getPermissions().stream().map(p -> p.getModule() + ":" + p.getAction()).collect(Collectors.toSet())
                : Set.of();

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .onLeave(user.getOnLeave())
                .leaveNote(user.getLeaveNote())
                .role(user.getRole())
                .permissions(perms)
                .build();
    }
}

