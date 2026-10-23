package com.syncflow.project.infrastructure.persistence;

import com.syncflow.project.application.port.out.SprintRepositoryPort;
import com.syncflow.repository.SprintRepository;
import com.syncflow.model.Sprint;
import com.syncflow.model.enums.SprintStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintPersistenceAdapter implements SprintRepositoryPort {

    private final SprintRepository repository;

    @Override
    public Sprint save(Sprint sprint) {
        return repository.save(sprint);
    }

    @Override
    public Optional<Sprint> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<Sprint> findAllById(Collection<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId) {
        return repository.findByProjectIdOrderByStartDateDesc(projectId);
    }

    @Override
    public Optional<Sprint> findFirstByProjectIdAndStatus(Long projectId, SprintStatus status) {
        return repository.findFirstByProjectIdAndStatus(projectId, status);
    }

    @Override
    public boolean existsByProjectIdAndStatus(Long projectId, SprintStatus status) {
        return repository.existsByProjectIdAndStatus(projectId, status);
    }

    @Override
    public List<Sprint> findTop3ByProjectIdAndStatusOrderByEndDateDesc(Long projectId, SprintStatus status) {
        return repository.findTop3ByProjectIdAndStatusOrderByEndDateDesc(projectId, status);
    }
}
