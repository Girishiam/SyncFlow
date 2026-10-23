package com.syncflow.notification.application.port.in;

import com.syncflow.model.enums.*;

import com.syncflow.dto.NotificationDTO;
import com.syncflow.dto.NotificationSummaryDTO;
import com.syncflow.model.Channel;
import com.syncflow.model.Message;
import com.syncflow.model.Task;
import com.syncflow.model.User;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationUseCase {
    List<NotificationDTO> getNotificationsForUser(UUID userId);
    NotificationSummaryDTO getSummaryForUser(UUID userId);
    NotificationDTO markAsRead(Long notificationId, UUID userId);
    void markAllAsRead(UUID userId);

    NotificationDTO notifyTaskAssigned(Task task, User assignee, User actor);
    void notifyBlockerAlert(Task task, String blockerReason, User actor);
    NotificationDTO notifyChannelMention(Channel channel, Message message, User sender, User recipient);
    void notifyTaskCompleted(Task task, User actor);

    /** Sends an individual notification to every active user holding one of {@code roleNames} who can access the task's project. */
    int notifyRolesForTask(Task task, Set<String> roleNames, String title, String message, NotificationType type,
                           NotificationPriority priority, User actor, String senderName);

    int scanAndGenerateDeadlineNotifications();
}
