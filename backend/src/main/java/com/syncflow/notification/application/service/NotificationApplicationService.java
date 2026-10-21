package com.syncflow.notification.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.NotificationDTO;
import com.syncflow.dto.NotificationSummaryDTO;
import com.syncflow.model.Channel;
import com.syncflow.model.Message;
import com.syncflow.model.Notification;
import com.syncflow.model.Role;
import com.syncflow.model.Task;
import com.syncflow.model.User;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.notification.application.port.out.NotificationRepositoryPort;
import com.syncflow.notification.application.port.out.NotificationWebSocketPort;
import com.syncflow.repository.RoleRepository;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationApplicationService implements NotificationUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationWebSocketPort notificationWebSocketPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TaskRepositoryPort taskRepositoryPort;
    private final RoleRepository roleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
            .withZone(ZoneId.systemDefault());

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsForUser(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));

        Role role = user.getRole();
        List<Notification> list = notificationRepositoryPort.findForUserAndRole(user, role);
        return list.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryDTO getSummaryForUser(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));

        Role role = user.getRole();
        long unreadTotal = notificationRepositoryPort.countUnreadForUserAndRole(user, role);
        long unreadMentions = notificationRepositoryPort.countUnreadByTypeForUserAndRole(user, role, "CHANNEL_MENTION");
        long unreadTasks = notificationRepositoryPort.countUnreadByTypeForUserAndRole(user, role, "TASK_ASSIGNED");
        long unreadDueSoon = notificationRepositoryPort.countUnreadByTypeForUserAndRole(user, role, "TASK_DUE_SOON");
        long unreadOverdue = notificationRepositoryPort.countUnreadByTypeForUserAndRole(user, role, "TASK_OVERDUE");
        long unreadBlockers = notificationRepositoryPort.countUnreadByTypeForUserAndRole(user, role, "BLOCKER_ALERT");

        return NotificationSummaryDTO.builder()
                .unreadTotal(unreadTotal)
                .unreadMentions(unreadMentions)
                .unreadTasks(unreadTasks)
                .unreadDeadlines(unreadDueSoon + unreadOverdue)
                .unreadBlockers(unreadBlockers)
                .build();
    }

    @Override
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, UUID userId) {
        Notification notification = notificationRepositoryPort.findById(notificationId)
                .orElseThrow(() -> new DomainException("Notification not found: " + notificationId));

        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        Notification saved = notificationRepositoryPort.save(notification);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));

        notificationRepositoryPort.markAllAsReadForUserAndRole(user, user.getRole(), Instant.now());
    }

    @Override
    @Transactional
    public NotificationDTO notifyTaskAssigned(Task task, User assignee, User actor) {
        if (assignee == null) return null;

        String priority = "INFO";
        if ("CRITICAL".equalsIgnoreCase(task.getPriority())) priority = "CRITICAL";
        else if ("HIGH".equalsIgnoreCase(task.getPriority())) priority = "WARNING";

        String actorName = (actor != null && actor.getFullName() != null) ? actor.getFullName() : "Bank Lead";
        String messageContent = String.format("%s assigned you to '%s' (%s priority). Due: %s",
                actorName,
                task.getTitle(),
                task.getPriority(),
                task.getDeadline() != null ? DATE_FORMATTER.format(task.getDeadline()) : "No strict deadline set"
        );

        Notification notification = Notification.builder()
                .recipient(assignee)
                .title("New Task Assigned: " + task.getTitle())
                .message(messageContent)
                .type("TASK_ASSIGNED")
                .priority(priority)
                .isRead(false)
                .actionType("NAVIGATE_TASK")
                .referenceId(task.getId())
                .senderName(actorName)
                .senderAvatar(getInitials(actorName))
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepositoryPort.save(notification);
        NotificationDTO dto = toDTO(saved);
        notificationWebSocketPort.pushToUser(assignee.getId().toString(), dto);
        return dto;
    }

    @Override
    @Transactional
    public void notifyBlockerAlert(Task task, String blockerReason, User actor) {
        String actorName = (actor != null && actor.getFullName() != null) ? actor.getFullName() : "Team Member";
        String alertMsg = String.format("BLOCKER on '%s': %s (Reported by %s)",
                task.getTitle(), blockerReason, actorName);

        // Save a role-targeted notification for TEAM_LEAD, REPORTING_MANAGER, and ADMIN
        List<String> targetRoles = List.of("TEAM_LEAD", "REPORTING_MANAGER", "ADMIN");
        for (String roleName : targetRoles) {
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role != null) {
                Notification notification = Notification.builder()
                        .targetRole(role)
                        .title("Sprint Blocker Alert: " + task.getTitle())
                        .message(alertMsg)
                        .type("BLOCKER_ALERT")
                        .priority("CRITICAL")
                        .isRead(false)
                        .actionType("NAVIGATE_TASK")
                        .referenceId(task.getId())
                        .senderName(actorName)
                        .senderAvatar(getInitials(actorName))
                        .createdAt(Instant.now())
                        .build();

                Notification saved = notificationRepositoryPort.save(notification);
                notificationWebSocketPort.pushToRole(roleName, toDTO(saved));
            }
        }
    }

    @Override
    @Transactional
    public NotificationDTO notifyChannelMention(Channel channel, Message message, User sender, User recipient) {
        if (recipient == null || (sender != null && recipient.getId().equals(sender.getId()))) {
            return null;
        }

        String senderName = sender != null ? sender.getFullName() : "Bank Member";
        String preview = message.getContent().length() > 90
                ? message.getContent().substring(0, 87) + "..."
                : message.getContent();

        Notification notification = Notification.builder()
                .recipient(recipient)
                .title("Mentioned in #" + channel.getName())
                .message(String.format("%s mentioned you in #%s: \"%s\"", senderName, channel.getName(), preview))
                .type("CHANNEL_MENTION")
                .priority("INFO")
                .isRead(false)
                .actionType("NAVIGATE_CHANNEL")
                .referenceId(channel.getId())
                .senderName(senderName)
                .senderAvatar(getInitials(senderName))
                .createdAt(Instant.now())
                .build();

        Notification saved = notificationRepositoryPort.save(notification);
        NotificationDTO dto = toDTO(saved);
        notificationWebSocketPort.pushToUser(recipient.getId().toString(), dto);
        return dto;
    }

    @Override
    @Transactional
    public void notifyTaskCompleted(Task task, User actor) {
        String actorName = actor != null ? actor.getFullName() :
                (task.getAssignee() != null ? task.getAssignee().getFullName() : "Team Member");
        String title = "Task Completed: " + task.getTitle();
        String message = String.format("%s completed and verified '%s' in %s.",
                actorName, task.getTitle(), task.getProject() != null ? task.getProject().getName() : "the project");

        List<String> targetRoles = List.of("TEAM_LEAD", "REPORTING_MANAGER", "ADMIN");
        for (String roleName : targetRoles) {
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role != null) {
                Notification notification = Notification.builder()
                        .targetRole(role)
                        .title(title)
                        .message(message)
                        .type("TASK_COMPLETED")
                        .priority("INFO")
                        .isRead(false)
                        .actionType("NAVIGATE_TASK")
                        .referenceId(task.getId())
                        .senderName(actorName)
                        .senderAvatar(getInitials(actorName))
                        .createdAt(Instant.now())
                        .build();

                Notification saved = notificationRepositoryPort.save(notification);
                notificationWebSocketPort.pushToRole(roleName, toDTO(saved));
            }
        }
    }

    @Override
    @Transactional
    public int scanAndGenerateDeadlineNotifications() {
        Instant now = Instant.now();
        Instant in24Hours = now.plus(Duration.ofHours(24));
        Instant oneDayAgo = now.minus(Duration.ofHours(24));

        List<Task> allTasks = taskRepositoryPort.findAll();
        int generatedCount = 0;

        for (Task task : allTasks) {
            if (task.getStage() != null && Boolean.TRUE.equals(task.getStage().getIsDoneStage())) {
                continue; // Finished tasks don't need deadline warnings
            }

            User assignee = task.getAssignee();
            if (assignee == null || task.getDeadline() == null) {
                continue;
            }

            Instant deadline = task.getDeadline();

            // 1. Task Overdue (Deadline passed)
            if (deadline.isBefore(now)) {
                boolean alreadySent = notificationRepositoryPort.hasRecentNotification(
                        task.getId(), "TASK_OVERDUE", assignee, oneDayAgo);

                if (!alreadySent) {
                    Notification n = Notification.builder()
                            .recipient(assignee)
                            .title("OVERDUE: " + task.getTitle())
                            .message(String.format("CRITICAL: Task '%s' is overdue since %s. Immediate delivery update required.",
                                    task.getTitle(), DATE_FORMATTER.format(deadline)))
                            .type("TASK_OVERDUE")
                            .priority("CRITICAL")
                            .isRead(false)
                            .actionType("NAVIGATE_TASK")
                            .referenceId(task.getId())
                            .senderName("SyncFlow Automated System")
                            .senderAvatar("SF")
                            .createdAt(now)
                            .build();

                    Notification saved = notificationRepositoryPort.save(n);
                    notificationWebSocketPort.pushToUser(assignee.getId().toString(), toDTO(saved));
                    generatedCount++;
                }
            }
            // 2. Task Due Soon (Due within next 24 hours)
            else if (deadline.isAfter(now) && deadline.isBefore(in24Hours)) {
                boolean alreadySent = notificationRepositoryPort.hasRecentNotification(
                        task.getId(), "TASK_DUE_SOON", assignee, oneDayAgo);

                if (!alreadySent) {
                    Notification n = Notification.builder()
                            .recipient(assignee)
                            .title("Due Soon: " + task.getTitle())
                            .message(String.format("Reminder: Task '%s' is due in less than 24 hours (%s).",
                                    task.getTitle(), DATE_FORMATTER.format(deadline)))
                            .type("TASK_DUE_SOON")
                            .priority("WARNING")
                            .isRead(false)
                            .actionType("NAVIGATE_TASK")
                            .referenceId(task.getId())
                            .senderName("SyncFlow Automated System")
                            .senderAvatar("SF")
                            .createdAt(now)
                            .build();

                    Notification saved = notificationRepositoryPort.save(n);
                    notificationWebSocketPort.pushToUser(assignee.getId().toString(), toDTO(saved));
                    generatedCount++;
                }
            }
        }

        log.info("Deadline scan complete. Generated {} deadline/overdue notifications.", generatedCount);
        return generatedCount;
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipient() != null ? n.getRecipient().getId() : null)
                .recipientName(n.getRecipient() != null ? n.getRecipient().getFullName() : null)
                .targetRole(n.getTargetRole() != null ? n.getTargetRole().getName() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .priority(n.getPriority())
                .isRead(n.getIsRead())
                .linkUrl(n.getLinkUrl())
                .actionType(n.getActionType())
                .referenceId(n.getReferenceId())
                .senderName(n.getSenderName())
                .senderAvatar(n.getSenderAvatar())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .timeAgo(formatTimeAgo(n.getCreatedAt()))
                .build();
    }

    private String formatTimeAgo(Instant instant) {
        if (instant == null) return "Just now";
        Duration d = Duration.between(instant, Instant.now());
        long seconds = d.getSeconds();

        if (seconds < 60) return "Just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days == 1) return "Yesterday";
        if (days < 7) return days + "d ago";
        return DATE_FORMATTER.format(instant);
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "US";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
