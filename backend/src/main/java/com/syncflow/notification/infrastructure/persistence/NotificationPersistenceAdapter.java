package com.syncflow.notification.infrastructure.persistence;

import com.syncflow.model.Notification;
import com.syncflow.model.Role;
import com.syncflow.model.User;
import com.syncflow.notification.application.port.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

    private final SpringDataNotificationRepository repository;

    @Override
    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Notification> findForUserAndRole(User user, Role role) {
        return repository.findForUserAndRole(user, role);
    }

    @Override
    public long countUnreadForUserAndRole(User user, Role role) {
        return repository.countUnreadForUserAndRole(user, role);
    }

    @Override
    public long countUnreadByTypeForUserAndRole(User user, Role role, String type) {
        return repository.countUnreadByTypeForUserAndRole(user, role, type);
    }

    @Override
    public void markAllAsReadForUserAndRole(User user, Role role, Instant now) {
        repository.markAllAsReadForUserAndRole(user, role, now);
    }

    @Override
    public boolean hasRecentNotification(Long referenceId, String type, User recipient, Instant since) {
        return repository.countRecentDuplicate(referenceId, type, recipient, since) > 0;
    }
}
