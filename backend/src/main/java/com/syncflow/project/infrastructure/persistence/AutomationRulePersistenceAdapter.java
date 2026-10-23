package com.syncflow.project.infrastructure.persistence;

import com.syncflow.project.application.port.out.AutomationRuleRepositoryPort;
import com.syncflow.repository.AutomationRuleRepository;
import com.syncflow.model.AutomationRule;
import com.syncflow.model.enums.AutomationTrigger;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationRulePersistenceAdapter implements AutomationRuleRepositoryPort {

    private final AutomationRuleRepository repository;

    @Override
    public AutomationRule save(AutomationRule rule) {
        return repository.save(rule);
    }

    @Override
    public Optional<AutomationRule> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<AutomationRule> findByProjectId(Long projectId) {
        return repository.findByProjectId(projectId);
    }

    @Override
    public List<AutomationRule> findByProjectIdAndTriggerEventAndIsEnabledTrue(Long projectId, AutomationTrigger trigger) {
        return repository.findByProjectIdAndTriggerEventAndIsEnabledTrue(projectId, trigger);
    }
}
