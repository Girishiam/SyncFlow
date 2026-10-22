package com.syncflow.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscriptionDTO {
    private Long id;
    private Long projectId;
    private String name;
    private String targetUrl;
    private String secretToken;
    private List<String> events;
    private Boolean isEnabled;
    private Integer deliveryCount;
    private Instant lastDeliveredAt;
    private String lastResponseStatus;
}
