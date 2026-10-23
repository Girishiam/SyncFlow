package com.syncflow.repository;

import com.syncflow.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {
    List<WebhookSubscription> findByProjectId(Long projectId);

    @Query("SELECT w.projectId FROM WebhookSubscription w WHERE w.id = :id")
    Optional<Long> findProjectIdById(@Param("id") Long id);
}
