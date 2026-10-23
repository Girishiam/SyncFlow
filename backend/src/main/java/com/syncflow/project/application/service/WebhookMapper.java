package com.syncflow.project.application.service;

import com.syncflow.dto.WebhookSubscriptionDTO;
import com.syncflow.model.WebhookSubscription;

import java.util.Arrays;
import java.util.List;

/** Maps webhook subscriptions to API DTOs; the signing secret is never returned in full. */
public final class WebhookMapper {

    private WebhookMapper() {
    }

    public static WebhookSubscriptionDTO toDTO(WebhookSubscription sub) {
        List<String> eventList = sub.getEvents() != null ? Arrays.asList(sub.getEvents().split(",")) : List.of();
        String secret = sub.getSecretToken();
        return WebhookSubscriptionDTO.builder()
                .id(sub.getId())
                .projectId(sub.getProjectId())
                .name(sub.getName())
                .targetUrl(sub.getTargetUrl())
                .secretToken(secret != null && !secret.isBlank()
                        ? "••••••••" + (secret.length() > 4 ? secret.substring(secret.length() - 4) : "")
                        : null)
                .events(eventList)
                .isEnabled(sub.getIsEnabled())
                .deliveryCount(sub.getDeliveryCount() != null ? sub.getDeliveryCount() : 0)
                .lastDeliveredAt(sub.getLastDeliveredAt())
                .lastResponseStatus(sub.getLastResponseStatus())
                .build();
    }
}
