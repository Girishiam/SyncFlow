package com.syncflow.notification.application.port.out;

import com.syncflow.model.Notification;
import com.syncflow.model.Role;
import com.syncflow.model.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findForUserAndRole(User user, Role role);
    long countUnreadForUserAndRole(User user, Role role);
    long countUnreadByTypeForUserAndRole(User user, Role role, String type);
    void markAllAsReadForUserAndRole(User user, Role role, Instant now);
    boolean hasRecentNotification(Long referenceId, String type, User recipient, Instant since);
}
