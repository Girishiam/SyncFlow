package com.syncflow.project.infrastructure.persistence;

import com.syncflow.project.application.port.out.WebhookSubscriptionRepositoryPort;
import com.syncflow.repository.WebhookSubscriptionRepository;
import com.syncflow.model.WebhookSubscription;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebhookSubscriptionPersistenceAdapter implements WebhookSubscriptionRepositoryPort {

    private final WebhookSubscriptionRepository repository;

    @Override
    public WebhookSubscription save(WebhookSubscription subscription) {
        return repository.save(subscription);
    }

    @Override
    public Optional<WebhookSubscription> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<WebhookSubscription> findByProjectId(Long projectId) {
        return repository.findByProjectId(projectId);
    }

    @Override
    public void delete(WebhookSubscription subscription) {
        repository.delete(subscription);
    }
}
