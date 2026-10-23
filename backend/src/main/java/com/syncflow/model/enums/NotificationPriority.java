package com.syncflow.model.enums;

public enum NotificationPriority {
    INFO, WARNING, CRITICAL;

    public static NotificationPriority forTask(TaskPriority priority) {
        if (priority == TaskPriority.CRITICAL) return CRITICAL;
        if (priority == TaskPriority.HIGH) return WARNING;
        return INFO;
    }
}
