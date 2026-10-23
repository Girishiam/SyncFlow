package com.syncflow.chat.application.service;

import com.syncflow.model.enums.*;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.chat.application.port.in.ChatUseCase;
import com.syncflow.chat.application.port.out.ChannelRepositoryPort;
import com.syncflow.chat.application.port.out.ChatNotificationPort;
import com.syncflow.chat.application.port.out.MessageRepositoryPort;
import com.syncflow.chat.application.port.out.MessageSeenReceiptRepositoryPort;
import com.syncflow.common.domain.ConflictException;
import com.syncflow.common.domain.DomainException;
import com.syncflow.common.domain.ForbiddenException;
import com.syncflow.common.domain.NotFoundException;
import com.syncflow.dto.ChatMessageDTO;
import com.syncflow.dto.CreateChannelRequest;
import com.syncflow.dto.UpdateChannelRequest;
import com.syncflow.dto.UserDTO;
import com.syncflow.model.*;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.security.CurrentUser;
import com.syncflow.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatApplicationService implements ChatUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final Pattern MENTION = Pattern.compile("@([\\p{L}\\p{N}._-]{2,64})");
    private static final Set<String> CHANNEL_WIDE = Set.of("channel", "here", "all");

    private final ChannelRepositoryPort channelRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final MessageSeenReceiptRepositoryPort receiptRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProjectRepositoryPort projectRepositoryPort;
    private final ChatNotificationPort chatNotificationPort;
    private final NotificationUseCase notificationUseCase;
    private final AuditLogUseCase auditLogUseCase;
    private final CurrentUser currentUser;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());

    @Override
    @Transactional(readOnly = true)
    public List<Channel> getAllChannels() {
        UserDetailsImpl user = currentUser.require();
        return CurrentUser.isAdmin(user)
                ? channelRepositoryPort.findAll()
                : channelRepositoryPort.findVisibleTo(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> getChannelMessages(Long channelId, Long beforeId, int limit) {
        Channel channel = requireChannel(channelId);
        int pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, limit));
        List<Message> newestFirst = messageRepositoryPort.findPage(channelId, beforeId, pageSize);

        Map<Long, List<String>> seenBy = receiptRepositoryPort
                .findByMessageIds(newestFirst.stream().map(Message::getId).toList()).stream()
                .collect(Collectors.groupingBy(r -> r.getMessage().getId(),
                        Collectors.mapping(r -> r.getUser().getFullName(), Collectors.toList())));

        List<ChatMessageDTO> result = new ArrayList<>(newestFirst.size());
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            Message msg = newestFirst.get(i);
            result.add(toDTO(channel, msg, seenBy.getOrDefault(msg.getId(), List.of())));
        }
        return result;
    }

    @Override
    @Transactional
    public ChatMessageDTO postMessage(Long channelId, String content) {
        Channel channel = requireChannel(channelId);
        User sender = actor();
        if (content == null || content.isBlank()) {
            throw new DomainException("Message cannot be empty");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new DomainException("Message must be at most " + MAX_MESSAGE_LENGTH + " characters");
        }

        Message saved = messageRepositoryPort.save(Message.builder()
                .channel(channel)
                .sender(sender)
                .content(content.trim())
                .isPinned(false)
                .build());

        ChatMessageDTO dto = toDTO(channel, saved, List.of());
        chatNotificationPort.broadcastMessage(channelId, dto);
        dispatchMentionNotifications(channel, saved, sender);
        return dto;
    }

    /**
     * Resolves {@code @name}, {@code @firstname.lastname}, {@code @emailprefix}, {@code @ROLE} and
     * {@code @channel/@here/@all}. In private channels only members are notified, since others
     * cannot read the message.
     */
    private void dispatchMentionNotifications(Channel channel, Message message, User sender) {
        Set<String> tokens = new HashSet<>();
        Matcher m = MENTION.matcher(message.getContent().toLowerCase(Locale.ROOT));
        while (m.find()) {
            tokens.add(m.group(1));
        }
        if (tokens.isEmpty()) return;

        boolean isPrivate = Boolean.TRUE.equals(channel.getIsPrivate());
        Set<UUID> memberIds = channel.getMembers().stream().map(User::getId).collect(Collectors.toSet());
        boolean channelWide = tokens.stream().anyMatch(CHANNEL_WIDE::contains);

        Map<UUID, User> recipients = new LinkedHashMap<>();
        if (channelWide) {
            // Channel members; a public channel without explicit members addresses every active user.
            Collection<User> audience = memberIds.isEmpty() && !isPrivate
                    ? userRepositoryPort.findByStatus(UserStatus.ACTIVE)
                    : channel.getMembers();
            audience.forEach(u -> recipients.put(u.getId(), u));
        }
        for (String token : tokens) {
            if (CHANNEL_WIDE.contains(token)) continue;
            userRepositoryPort.findActiveByMentionToken(token).forEach(u -> recipients.put(u.getId(), u));
        }

        for (User u : recipients.values()) {
            if (u.getId().equals(sender.getId()) || u.getStatus() != UserStatus.ACTIVE) continue;
            if (isPrivate && !memberIds.contains(u.getId())) continue;
            notificationUseCase.notifyChannelMention(channel, message, sender, u);
        }
    }

    @Override
    @Transactional
    public Channel createChannel(CreateChannelRequest request) {
        String cleanName = slug(request.getName());
        if (channelRepositoryPort.existsByName(cleanName)) {
            throw new ConflictException("Channel with name #" + cleanName + " already exists");
        }

        User creator = actor();
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepositoryPort.findById(request.getProjectId())
                    .orElseThrow(() -> new NotFoundException("Project not found: " + request.getProjectId()));
        }

        Set<User> members = new HashSet<>();
        members.add(creator);
        if (request.getMemberIds() != null) {
            for (UUID memberId : request.getMemberIds()) {
                members.add(userRepositoryPort.findById(memberId)
                        .orElseThrow(() -> new NotFoundException("User not found: " + memberId)));
            }
        }

        Channel saved = channelRepositoryPort.save(Channel.builder()
                .name(cleanName)
                .description(request.getDescription())
                .type(ChannelType.parseOrDefault(request.getType()))
                .icon(request.getIcon() != null && !request.getIcon().isBlank() ? request.getIcon() : "hash")
                .isPrivate(Boolean.TRUE.equals(request.getIsPrivate()))
                .project(project)
                .createdBy(creator)
                .members(members)
                .build());
        auditLogUseCase.recordLog("CHANNEL_CREATED", "CHANNEL", saved.getId(), saved.getName(), null, null, null,
                (Boolean.TRUE.equals(saved.getIsPrivate()) ? "Private" : "Public") + " channel created");
        return saved;
    }

    @Override
    @Transactional
    public Channel updateChannel(Long channelId, UpdateChannelRequest request) {
        Channel channel = requireChannel(channelId);

        if (request.getName() != null && !request.getName().isBlank()) {
            String cleanName = slug(request.getName());
            if (!channel.getName().equalsIgnoreCase(cleanName)) {
                if ("general".equalsIgnoreCase(channel.getName())) {
                    throw new DomainException("The default #general channel name cannot be modified.");
                }
                if (channelRepositoryPort.existsByName(cleanName)) {
                    throw new ConflictException("Channel with name #" + cleanName + " already exists");
                }
                channel.setName(cleanName);
            }
        }
        if (request.getDescription() != null) channel.setDescription(request.getDescription());
        if (request.getType() != null && !request.getType().isBlank()) channel.setType(ChannelType.parseOrDefault(request.getType()));
        if (request.getIcon() != null && !request.getIcon().isBlank()) channel.setIcon(request.getIcon());
        if (request.getIsPrivate() != null) channel.setIsPrivate(request.getIsPrivate());

        return channelRepositoryPort.save(channel);
    }

    @Override
    @Transactional
    public void deleteChannel(Long channelId) {
        Channel channel = requireChannel(channelId);
        if ("general".equalsIgnoreCase(channel.getName())) {
            throw new DomainException("The default #general channel cannot be deleted.");
        }
        receiptRepositoryPort.deleteByChannelId(channelId);
        messageRepositoryPort.deleteByChannelId(channelId);
        channel.getMembers().clear();
        channelRepositoryPort.deleteById(channelId);
        auditLogUseCase.recordLog("CHANNEL_DELETED", "CHANNEL", channelId, channel.getName(), null, null, null,
                "Channel and its message history deleted");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getChannelMembers(Long channelId) {
        return requireChannel(channelId).getMembers().stream().map(this::toUserDTO).toList();
    }

    @Override
    @Transactional
    public void addMemberToChannel(Long channelId, UUID memberId) {
        Channel channel = requireChannel(channelId);
        User member = userRepositoryPort.findById(memberId)
                .orElseThrow(() -> new NotFoundException("User not found: " + memberId));
        channel.getMembers().add(member);
        channelRepositoryPort.save(channel);
        auditLogUseCase.recordLog("CHANNEL_MEMBER_ADDED", "CHANNEL", channelId, channel.getName(), null, null, null,
                "Added " + member.getEmail());
    }

    @Override
    @Transactional
    public void removeMemberFromChannel(Long channelId, UUID memberId) {
        Channel channel = requireChannel(channelId);
        User member = userRepositoryPort.findById(memberId)
                .orElseThrow(() -> new NotFoundException("User not found: " + memberId));
        channel.getMembers().remove(member);
        channelRepositoryPort.save(channel);
        auditLogUseCase.recordLog("CHANNEL_MEMBER_REMOVED", "CHANNEL", channelId, channel.getName(), null, null, null,
                "Removed " + member.getEmail());
    }

    private Channel requireChannel(Long channelId) {
        return channelRepositoryPort.findById(channelId)
                .orElseThrow(() -> new NotFoundException("Channel not found: " + channelId));
    }

    private User actor() {
        UserDetailsImpl details = currentUser.require();
        return userRepositoryPort.findById(details.getId())
                .orElseThrow(() -> new ForbiddenException("Unknown user"));
    }

    private static String slug(String rawName) {
        String raw = rawName != null ? rawName : "";
        String clean = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        if (clean.isBlank()) {
            throw new DomainException("Channel name must contain letters or digits");
        }
        return clean.length() > 80 ? clean.substring(0, 80) : clean;
    }

    private ChatMessageDTO toDTO(Channel channel, Message msg, List<String> seenBy) {
        return ChatMessageDTO.builder()
                .id(msg.getId())
                .channelId(channel.getId())
                .channelName(channel.getName())
                .senderId(msg.getSender().getId())
                .sender(msg.getSender().getFullName())
                .text(msg.getContent())
                .time(TIME_FORMATTER.format(msg.getCreatedAt()))
                .seenBy(seenBy)
                .build();
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .onLeave(user.getOnLeave())
                .leaveNote(user.getLeaveNote())
                .role(user.getRole())
                .permissions(Set.of())
                .build();
    }
}
