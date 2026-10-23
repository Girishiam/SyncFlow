package com.syncflow.notification.application.service;

import com.syncflow.project.application.port.out.ProjectRepositoryPort;

import com.syncflow.model.enums.*;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.common.domain.NotFoundException;
import com.syncflow.dto.NotificationDTO;
import com.syncflow.dto.NotificationSummaryDTO;
import com.syncflow.model.Channel;
import com.syncflow.model.Message;
import com.syncflow.model.Notification;
import com.syncflow.model.Project;
import com.syncflow.model.Task;
import com.syncflow.model.User;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.notification.application.port.out.NotificationRepositoryPort;
import com.syncflow.notification.application.port.out.NotificationWebSocketPort;
import com.syncflow.security.Permissions;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationApplicationService implements NotificationUseCase {

    private static final int MAX_NOTIFICATIONS = 100;
    private static final Set<String> LEADERSHIP_ROLES = Set.of("TEAM_LEAD", "REPORTING_MANAGER", Permissions.ADMIN_ROLE);

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationWebSocketPort notificationWebSocketPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ProjectRepositoryPort projectRepository;
    private final TaskRepositoryPort taskRepositoryPort;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
            .withZone(ZoneId.systemDefault());

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsForUser(UUID userId) {
        User user = requireUser(userId);
        return notificationRepositoryPort.findForRecipient(user, MAX_NOTIFICATIONS).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryDTO getSummaryForUser(UUID userId) {
        User user = requireUser(userId);
        long unreadTotal = notificationRepositoryPort.countUnread(user);
        long unreadMentions = notificationRepositoryPort.countUnreadByType(user, NotificationType.CHANNEL_MENTION);
        long unreadTasks = notificationRepositoryPort.countUnreadByType(user, NotificationType.TASK_ASSIGNED);
        long unreadDueSoon = notificationRepositoryPort.countUnreadByType(user, NotificationType.TASK_DUE_SOON);
        long unreadOverdue = notificationRepositoryPort.countUnreadByType(user, NotificationType.TASK_OVERDUE);
        long unreadBlockers = notificationRepositoryPort.countUnreadByType(user, NotificationType.BLOCKER_ALERT);

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
        User user = requireUser(userId);
        Notification notification = notificationRepositoryPort.findById(notificationId)
                .filter(n -> n.getRecipient().getId().equals(user.getId()))
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        notification.setIsRead(true);
        notification.setReadAt(Instant.now());
        return toDTO(notificationRepositoryPort.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = requireUser(userId);
        notificationRepositoryPort.markAllAsRead(user, Instant.now());
    }

    @Override
    @Transactional
    public NotificationDTO notifyAddedToProject(Project project, User member, User actor) {
        if (member == null || (actor != null && actor.getId().equals(member.getId()))) {
            return null;
        }
        String actorName = (actor != null && actor.getFullName() != null) ? actor.getFullName() : "SyncFlow";
        return send(Notification.builder()
                .recipient(member)
                .title("Added to " + project.getName())
                .message(actorName + " added you to the " + project.getName() + " team.")
                .type(NotificationType.PROJECT_ADDED)
                .priority(NotificationPriority.INFO)
                .actionType(NotificationAction.NAVIGATE_PROJECT)
                .referenceId(project.getId())
                .senderName(actorName)
                .senderAvatar(getInitials(actorName))
                .build());
    }

    @Override
    @Transactional
    public NotificationDTO notifyTaskAssigned(Task task, User assignee, User actor) {
        if (assignee == null || (actor != null && actor.getId().equals(assignee.getId()))) {
            return null;
        }

        String actorName = (actor != null && actor.getFullName() != null) ? actor.getFullName() : "SyncFlow";
        String messageContent = String.format("%s assigned you to '%s' (%s priority). Due: %s",
                actorName,
                task.getTitle(),
                task.getPriority(),
                task.getDeadline() != null ? DATE_FORMATTER.format(task.getDeadline()) : "No deadline set"
        );

        return send(Notification.builder()
                .recipient(assignee)
                .title("New Task Assigned: " + task.getTitle())
                .message(messageContent)
                .type(NotificationType.TASK_ASSIGNED)
                .priority(NotificationPriority.forTask(task.getPriority()))
                .actionType(NotificationAction.NAVIGATE_TASK)
                .referenceId(task.getId())
                .senderName(actorName)
                .senderAvatar(getInitials(actorName))
                .build());
    }

    @Override
    @Transactional
    public void notifyBlockerAlert(Task task, String blockerReason, User actor) {
        String actorName = (actor != null && actor.getFullName() != null) ? actor.getFullName() : "Team Member";
        String message = String.format("BLOCKER on '%s': %s (Reported by %s)",
                task.getTitle(), blockerReason != null ? blockerReason : "No reason given", actorName);
        notifyRolesForTask(task, LEADERSHIP_ROLES, "Sprint Blocker Alert: " + task.getTitle(), message,
                NotificationType.BLOCKER_ALERT, NotificationPriority.CRITICAL, actor, actorName);
    }

    @Override
    @Transactional
    public NotificationDTO notifyChannelMention(Channel channel, Message message, User sender, User recipient) {
        if (recipient == null || (sender != null && recipient.getId().equals(sender.getId()))) {
            return null;
        }

        String senderName = sender != null ? sender.getFullName() : "Team Member";
        String preview = message.getContent().length() > 90
                ? message.getContent().substring(0, 87) + "..."
                : message.getContent();

        return send(Notification.builder()
                .recipient(recipient)
                .title("Mentioned in #" + channel.getName())
                .message(String.format("%s mentioned you in #%s: \"%s\"", senderName, channel.getName(), preview))
                .type(NotificationType.CHANNEL_MENTION)
                .priority(NotificationPriority.INFO)
                .actionType(NotificationAction.NAVIGATE_CHANNEL)
                .referenceId(channel.getId())
                .senderName(senderName)
                .senderAvatar(getInitials(senderName))
                .build());
    }

    @Override
    @Transactional
    public void notifyTaskCompleted(Task task, User actor) {
        String actorName = actor != null ? actor.getFullName() :
                (task.getAssignee() != null ? task.getAssignee().getFullName() : "Team Member");
        String message = String.format("%s completed '%s' in %s.",
                actorName, task.getTitle(), task.getProject() != null ? task.getProject().getName() : "the project");
        notifyRolesForTask(task, LEADERSHIP_ROLES, "Task Completed: " + task.getTitle(), message,
                NotificationType.TASK_COMPLETED, NotificationPriority.INFO, actor, actorName);
    }

    @Override
    @Transactional
    public int notifyRolesForTask(Task task, Set<String> roleNames, String title, String message, NotificationType type,
                                  NotificationPriority priority, User actor, String senderName) {
        Long projectId = task.getProject() != null ? task.getProject().getId() : null;
        int sent = 0;
        for (String roleName : roleNames) {
            for (User recipient : userRepositoryPort.findByRoleNameAndStatus(roleName, UserStatus.ACTIVE)) {
                if (actor != null && recipient.getId().equals(actor.getId())) continue;
                boolean isAdmin = Permissions.ADMIN_ROLE.equalsIgnoreCase(roleName);
                if (projectId != null && !isAdmin && !projectRepository.isAccessibleBy(projectId, recipient.getId())) {
                    continue;
                }
                send(Notification.builder()
                        .recipient(recipient)
                        .title(title)
                        .message(message)
                        .type(type)
                        .priority(priority)
                        .actionType(NotificationAction.NAVIGATE_TASK)
                        .referenceId(task.getId())
                        .senderName(senderName)
                        .senderAvatar(getInitials(senderName))
                        .build());
                sent++;
            }
        }
        return sent;
    }

    @Override
    @Transactional
    public int scanAndGenerateDeadlineNotifications() {
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(Duration.ofHours(24));
        int generatedCount = 0;

        for (Task task : taskRepositoryPort.findOpenAssignedWithDeadlineBefore(now.plus(Duration.ofHours(24)))) {
            User assignee = task.getAssignee();
            Instant deadline = task.getDeadline();
            boolean overdue = deadline.isBefore(now);
            NotificationType type = overdue ? NotificationType.TASK_OVERDUE : NotificationType.TASK_DUE_SOON;

            if (notificationRepositoryPort.hasRecentNotification(task.getId(), type, assignee, oneDayAgo)) {
                continue;
            }

            send(Notification.builder()
                    .recipient(assignee)
                    .title((overdue ? "OVERDUE: " : "Due Soon: ") + task.getTitle())
                    .message(overdue
                            ? String.format("Task '%s' has been overdue since %s.", task.getTitle(), DATE_FORMATTER.format(deadline))
                            : String.format("Task '%s' is due in less than 24 hours (%s).", task.getTitle(), DATE_FORMATTER.format(deadline)))
                    .type(type)
                    .priority(overdue ? NotificationPriority.CRITICAL : NotificationPriority.WARNING)
                    .actionType(NotificationAction.NAVIGATE_TASK)
                    .referenceId(task.getId())
                    .senderName("SyncFlow Automated System")
                    .senderAvatar("SF")
                    .build());
            generatedCount++;
        }

        if (generatedCount > 0) {
            log.info("Deadline scan generated {} deadline/overdue notifications.", generatedCount);
        }
        return generatedCount;
    }

    private NotificationDTO send(Notification notification) {
        notification.setIsRead(false);
        notification.setCreatedAt(Instant.now());
        Notification saved = notificationRepositoryPort.save(notification);
        NotificationDTO dto = toDTO(saved);
        notificationWebSocketPort.pushToUser(saved.getRecipient().getEmail(), dto);
        return dto;
    }

    private User requireUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .recipientId(n.getRecipient() != null ? n.getRecipient().getId() : null)
                .recipientName(n.getRecipient() != null ? n.getRecipient().getFullName() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .priority(n.getPriority() != null ? n.getPriority().name() : null)
                .isRead(n.getIsRead())
                .linkUrl(n.getLinkUrl())
                .actionType(n.getActionType() != null ? n.getActionType().name() : null)
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
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
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
