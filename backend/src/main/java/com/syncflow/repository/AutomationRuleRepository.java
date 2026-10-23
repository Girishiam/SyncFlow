package com.syncflow.repository;

import com.syncflow.model.enums.*;

import com.syncflow.model.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {
    List<AutomationRule> findByProjectId(Long projectId);
    List<AutomationRule> findByProjectIdAndTriggerEventAndIsEnabledTrue(Long projectId, AutomationTrigger triggerEvent);

    @Query("SELECT r.projectId FROM AutomationRule r WHERE r.id = :id")
    Optional<Long> findProjectIdById(@Param("id") Long id);
}
