package com.syncflow.notification.infrastructure.persistence;

import com.syncflow.model.Notification;
import com.syncflow.model.Role;
import com.syncflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE (n.recipient = :user) OR (n.recipient IS NULL AND n.targetRole = :role) OR (n.recipient IS NULL AND n.targetRole IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findForUserAndRole(@Param("user") User user, @Param("role") Role role);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false AND ((n.recipient = :user) OR (n.recipient IS NULL AND n.targetRole = :role) OR (n.recipient IS NULL AND n.targetRole IS NULL))")
    long countUnreadForUserAndRole(@Param("user") User user, @Param("role") Role role);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false AND n.type = :type AND ((n.recipient = :user) OR (n.recipient IS NULL AND n.targetRole = :role) OR (n.recipient IS NULL AND n.targetRole IS NULL))")
    long countUnreadByTypeForUserAndRole(@Param("user") User user, @Param("role") Role role, @Param("type") String type);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.isRead = false AND ((n.recipient = :user) OR (n.recipient IS NULL AND n.targetRole = :role) OR (n.recipient IS NULL AND n.targetRole IS NULL))")
    void markAllAsReadForUserAndRole(@Param("user") User user, @Param("role") Role role, @Param("now") Instant now);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.referenceId = :referenceId AND n.type = :type AND n.recipient = :recipient AND n.createdAt >= :since")
    long countRecentDuplicate(@Param("referenceId") Long referenceId, @Param("type") String type, @Param("recipient") User recipient, @Param("since") Instant since);
}
