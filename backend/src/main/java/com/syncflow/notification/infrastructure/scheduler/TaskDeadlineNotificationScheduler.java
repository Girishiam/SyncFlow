package com.syncflow.notification.infrastructure.scheduler;

import com.syncflow.notification.application.port.in.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDeadlineNotificationScheduler {

    private final NotificationUseCase notificationUseCase;

    // Run 5 seconds after startup to prime any immediate due/overdue notices
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Running initial startup task deadline scan...");
        try {
            notificationUseCase.scanAndGenerateDeadlineNotifications();
        } catch (Exception e) {
            log.error("Failed startup deadline scan: {}", e.getMessage());
        }
    }

    // Run every 2 minutes
    @Scheduled(fixedRate = 120000)
    public void runPeriodicDeadlineScan() {
        try {
            notificationUseCase.scanAndGenerateDeadlineNotifications();
        } catch (Exception e) {
            log.error("Periodic deadline scan error: {}", e.getMessage());
        }
    }
}
