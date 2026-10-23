package com.syncflow.notification.infrastructure.persistence;

import com.syncflow.model.Notification;
import com.syncflow.model.User;
import com.syncflow.model.enums.NotificationType;
import com.syncflow.notification.application.port.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    public List<Notification> findForRecipient(User recipient, int limit) {
        return repository.findByRecipientOrderByCreatedAtDesc(recipient, PageRequest.of(0, limit));
    }

    @Override
    public long countUnread(User recipient) {
        return repository.countByRecipientAndIsReadFalse(recipient);
    }

    @Override
    public long countUnreadByType(User recipient, NotificationType type) {
        return repository.countByRecipientAndTypeAndIsReadFalse(recipient, type);
    }

    @Override
    public void markAllAsRead(User recipient, Instant now) {
        repository.markAllAsRead(recipient, now);
    }

    @Override
    public boolean hasRecentNotification(Long referenceId, NotificationType type, User recipient, Instant since) {
        return repository.existsByReferenceIdAndTypeAndRecipientAndCreatedAtGreaterThanEqual(referenceId, type, recipient, since);
    }
}
