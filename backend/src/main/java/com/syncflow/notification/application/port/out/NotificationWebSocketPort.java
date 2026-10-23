package com.syncflow.notification.application.port.out;

import com.syncflow.dto.NotificationDTO;

public interface NotificationWebSocketPort {
    /** Delivers to the user's private queue; {@code principalName} is the account email. */
    void pushToUser(String principalName, NotificationDTO notification);
}
