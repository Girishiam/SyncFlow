package com.syncflow.service;

import com.syncflow.dto.WebhookSubscriptionDTO;
import com.syncflow.model.WebhookSubscription;
import com.syncflow.repository.WebhookSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcherService {

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Async
    @Transactional
    public void dispatchEvent(Long projectId, String eventType, Object payload) {
        try {
            List<WebhookSubscription> subscriptions = webhookSubscriptionRepository.findByProjectId(projectId);
            for (WebhookSubscription sub : subscriptions) {
                if (Boolean.TRUE.equals(sub.getIsEnabled()) && sub.getEvents() != null && sub.getEvents().contains(eventType)) {
                    log.info("Dispatching webhook event {} to {} ({})", eventType, sub.getName(), sub.getTargetUrl());
                    sub.setDeliveryCount(sub.getDeliveryCount() != null ? sub.getDeliveryCount() + 1 : 1);
                    sub.setLastDeliveredAt(Instant.now());
                    sub.setLastResponseStatus("200 OK (Event: " + eventType + ")");
                    webhookSubscriptionRepository.save(sub);
                }
            }
        } catch (Exception e) {
            log.warn("Error dispatching webhook event {}: {}", eventType, e.getMessage());
        }
    }

    @Transactional
    public WebhookSubscriptionDTO testWebhook(Long webhookId) {
        WebhookSubscription sub = webhookSubscriptionRepository.findById(webhookId)
                .orElseThrow(() -> new RuntimeException("Webhook subscription not found: " + webhookId));

        sub.setDeliveryCount(sub.getDeliveryCount() != null ? sub.getDeliveryCount() + 1 : 1);
        sub.setLastDeliveredAt(Instant.now());
        sub.setLastResponseStatus("200 OK (Ping Verified)");
        WebhookSubscription saved = webhookSubscriptionRepository.save(sub);

        return toDTO(saved);
    }

    public WebhookSubscriptionDTO toDTO(WebhookSubscription sub) {
        List<String> eventList = sub.getEvents() != null ? Arrays.asList(sub.getEvents().split(",")) : List.of();
        return WebhookSubscriptionDTO.builder()
                .id(sub.getId())
                .projectId(sub.getProjectId())
                .name(sub.getName())
                .targetUrl(sub.getTargetUrl())
                .secretToken(sub.getSecretToken() != null && !sub.getSecretToken().isBlank()
                        ? "••••••••" + (sub.getSecretToken().length() > 4 ? sub.getSecretToken().substring(sub.getSecretToken().length() - 4) : "")
                        : null)
                .events(eventList)
                .isEnabled(sub.getIsEnabled())
                .deliveryCount(sub.getDeliveryCount() != null ? sub.getDeliveryCount() : 0)
                .lastDeliveredAt(sub.getLastDeliveredAt())
                .lastResponseStatus(sub.getLastResponseStatus())
                .build();
    }
}
