package com.syncflow.notification.application.port.out;

import com.syncflow.model.Notification;
import com.syncflow.model.User;
import com.syncflow.model.enums.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findForRecipient(User recipient, int limit);
    long countUnread(User recipient);
    long countUnreadByType(User recipient, NotificationType type);
    void markAllAsRead(User recipient, Instant now);
    boolean hasRecentNotification(Long referenceId, NotificationType type, User recipient, Instant since);
}
