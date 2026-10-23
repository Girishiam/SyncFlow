package com.syncflow.project.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncflow.common.domain.DomainException;
import com.syncflow.common.domain.NotFoundException;
import com.syncflow.common.event.ProjectEvent;
import com.syncflow.config.SyncFlowProperties;
import com.syncflow.project.application.port.out.WebhookDeliveryPort;
import com.syncflow.model.WebhookSubscription;
import com.syncflow.repository.WebhookSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Delivers project events to subscribed HTTP endpoints.
 *
 * <p>Each delivery is a JSON POST signed with HMAC-SHA256 over {@code "<timestamp>.<body>"} using
 * the subscription secret ({@code X-SyncFlow-Signature: sha256=<hex>}). Transient failures
 * (network errors, 429, 5xx) are retried with backoff. Targets resolving to loopback or private
 * networks are refused unless {@code syncflow.webhooks.allow-private-targets} is enabled.</p>
 */
@Service
@Slf4j
public class WebhookDispatcherService implements WebhookDeliveryPort {

    private static final int STATUS_MAX_LENGTH = 50;

    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final SyncFlowProperties.Webhooks config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WebhookDispatcherService(WebhookSubscriptionRepository webhookSubscriptionRepository,
                                    SyncFlowProperties properties,
                                    ObjectMapper objectMapper) {
        this.webhookSubscriptionRepository = webhookSubscriptionRepository;
        this.config = properties.getWebhooks();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProjectEvent(ProjectEvent event) {
        List<WebhookSubscription> subscriptions = webhookSubscriptionRepository.findByProjectId(event.projectId());
        for (WebhookSubscription sub : subscriptions) {
            if (Boolean.TRUE.equals(sub.getIsEnabled()) && subscribedEvents(sub).contains(event.type())) {
                deliver(sub, event, config.getMaxAttempts());
            }
        }
    }

    @Override
    public void validateTarget(String url) {
        validateTarget(url, config.isAllowPrivateTargets());
    }

    @Override
    public WebhookSubscription sendTestPing(Long webhookId) {
        WebhookSubscription sub = webhookSubscriptionRepository.findById(webhookId)
                .orElseThrow(() -> new NotFoundException("Webhook subscription not found: " + webhookId));
        deliver(sub, ProjectEvent.of(sub.getProjectId(), "PING", Map.of("message", "SyncFlow webhook test")), 1);
        return webhookSubscriptionRepository.findById(webhookId).orElse(sub);
    }

    private void deliver(WebhookSubscription sub, ProjectEvent event, int maxAttempts) {
        String outcome;
        try {
            URI target = validateTarget(sub.getTargetUrl(), config.isAllowPrivateTargets());
            String deliveryId = UUID.randomUUID().toString();
            String body = objectMapper.writeValueAsString(Map.of(
                    "id", deliveryId,
                    "event", event.type(),
                    "projectId", event.projectId(),
                    "occurredAt", event.occurredAt().toString(),
                    "data", event.payload()));
            outcome = sendWithRetry(target, sub.getSecretToken(), event.type(), deliveryId, body, maxAttempts);
        } catch (DomainException e) {
            outcome = "REFUSED: " + e.getMessage();
        } catch (Exception e) {
            outcome = "ERROR: " + e.getClass().getSimpleName();
            log.warn("Webhook {} delivery failed: {}", sub.getId(), e.getMessage());
        }

        sub.setDeliveryCount((sub.getDeliveryCount() != null ? sub.getDeliveryCount() : 0) + 1);
        sub.setLastDeliveredAt(Instant.now());
        sub.setLastResponseStatus(truncate(outcome));
        webhookSubscriptionRepository.save(sub);
        log.info("Webhook {} ({}) event {} -> {}", sub.getId(), sub.getName(), event.type(), outcome);
    }

    private String sendWithRetry(URI target, String secret, String eventType, String deliveryId, String body,
                                 int maxAttempts) throws InterruptedException {
        String outcome = "NOT_SENT";
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                long timestamp = Instant.now().getEpochSecond();
                HttpRequest.Builder request = HttpRequest.newBuilder(target)
                        .timeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "SyncFlow-Webhooks/1.0")
                        .header("X-SyncFlow-Event", eventType)
                        .header("X-SyncFlow-Delivery", deliveryId)
                        .header("X-SyncFlow-Timestamp", String.valueOf(timestamp))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                if (secret != null && !secret.isBlank()) {
                    request.header("X-SyncFlow-Signature", "sha256=" + sign(secret, timestamp + "." + body));
                }
                HttpResponse<Void> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                outcome = "HTTP " + status + (attempt > 1 ? " (attempt " + attempt + ")" : "");
                if (status < 500 && status != 429) {
                    return outcome;
                }
            } catch (IOException e) {
                outcome = "ERROR: " + e.getClass().getSimpleName() + (attempt > 1 ? " (attempt " + attempt + ")" : "");
            }
            if (attempt < maxAttempts) {
                Thread.sleep(1000L * attempt * attempt);
            }
        }
        return outcome;
    }

    /**
     * Validates a webhook target URL: http(s) only and, unless explicitly allowed, no loopback,
     * link-local, site-local, multicast or unique-local IPv6 destinations.
     */
    public static URI validateTarget(String url, boolean allowPrivateTargets) {
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException | NullPointerException e) {
            throw new DomainException("Webhook URL is not a valid URI");
        }
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new DomainException("Webhook URL must use http or https");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new DomainException("Webhook URL must include a host and no credentials");
        }
        if (!allowPrivateTargets) {
            try {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                    if (isInternal(address)) {
                        throw new DomainException("Webhook URL resolves to a private or loopback address");
                    }
                }
            } catch (UnknownHostException e) {
                throw new DomainException("Webhook host cannot be resolved");
            }
        }
        return uri;
    }

    private static boolean isInternal(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet6Address) {
            byte first = address.getAddress()[0];
            return (first & 0xFE) == 0xFC; // fc00::/7 unique local
        }
        byte[] b = address.getAddress();
        // 100.64.0.0/10 carrier-grade NAT and 169.254.0.0/16 cloud metadata ranges
        return (b[0] == 100 && (b[1] & 0xC0) == 64) || ((b[0] & 0xFF) == 169 && (b[1] & 0xFF) == 254);
    }

    static String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign webhook payload", e);
        }
    }

    private static Set<String> subscribedEvents(WebhookSubscription sub) {
        if (sub.getEvents() == null || sub.getEvents().isBlank()) return Set.of();
        Set<String> events = new HashSet<>();
        for (String e : sub.getEvents().split(",")) {
            events.add(e.trim().toUpperCase(Locale.ROOT));
        }
        return events;
    }

    private static String truncate(String s) {
        return s.length() <= STATUS_MAX_LENGTH ? s : s.substring(0, STATUS_MAX_LENGTH);
    }

}
