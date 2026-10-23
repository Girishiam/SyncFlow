package com.syncflow.project.application.port.out;

import com.syncflow.model.WebhookSubscription;

/** Outbound HTTP delivery of project events to subscribed endpoints. */
public interface WebhookDeliveryPort {

    /** Rejects URLs that are not http(s) or (unless allowed by configuration) resolve to private networks. */
    void validateTarget(String url);

    /** Sends a signed PING synchronously and returns the subscription with the recorded outcome. */
    WebhookSubscription sendTestPing(Long webhookId);
}
