package com.syncflow.repository;

import com.syncflow.model.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {
    List<AutomationRule> findByProjectId(Long projectId);
    List<AutomationRule> findByProjectIdAndTriggerEventAndIsEnabledTrue(Long projectId, String triggerEvent);
}
