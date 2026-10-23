package com.syncflow.project.application.port.out;

import com.syncflow.model.WebhookSubscription;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link WebhookSubscription}. */
public interface WebhookSubscriptionRepositoryPort {
    WebhookSubscription save(WebhookSubscription subscription);
    Optional<WebhookSubscription> findById(Long id);
    List<WebhookSubscription> findByProjectId(Long projectId);
    void delete(WebhookSubscription subscription);
}
