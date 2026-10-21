package com.syncflow.notification.adapter.web;

import com.syncflow.dto.NotificationDTO;
import com.syncflow.dto.NotificationSummaryDTO;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationUseCase.getNotificationsForUser(userDetails.getId()));
    }

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryDTO> getSummary(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationUseCase.getSummaryForUser(userDetails.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationUseCase.markAsRead(id, userDetails.getId()));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        notificationUseCase.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @PostMapping("/trigger-due-check")
    public ResponseEntity<Map<String, Object>> triggerDueCheck() {
        int count = notificationUseCase.scanAndGenerateDeadlineNotifications();
        return ResponseEntity.ok(Map.of(
                "scannedCount", count,
                "message", "Deadline scan completed successfully"
        ));
    }
}
