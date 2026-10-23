package com.syncflow.project.application.port.out;

import com.syncflow.model.AutomationRule;
import com.syncflow.model.enums.AutomationTrigger;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link AutomationRule}. */
public interface AutomationRuleRepositoryPort {
    AutomationRule save(AutomationRule rule);
    Optional<AutomationRule> findById(Long id);
    List<AutomationRule> findByProjectId(Long projectId);
    List<AutomationRule> findByProjectIdAndTriggerEventAndIsEnabledTrue(Long projectId, AutomationTrigger trigger);
}
