package com.syncflow.notification.application.port.out;

import com.syncflow.dto.NotificationDTO;

public interface NotificationWebSocketPort {
    void pushToUser(String userId, NotificationDTO notification);
    void pushToRole(String roleName, NotificationDTO notification);
    void broadcast(NotificationDTO notification);
}
