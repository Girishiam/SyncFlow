package com.syncflow.notification.infrastructure.persistence;

import com.syncflow.model.Notification;
import com.syncflow.model.User;
import com.syncflow.model.enums.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    long countByRecipientAndIsReadFalse(User recipient);

    long countByRecipientAndTypeAndIsReadFalse(User recipient, NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.isRead = false AND n.recipient = :user")
    int markAllAsRead(@Param("user") User user, @Param("now") Instant now);

    boolean existsByReferenceIdAndTypeAndRecipientAndCreatedAtGreaterThanEqual(
            Long referenceId, NotificationType type, User recipient, Instant since);
}
