package com.syncflow.project.infrastructure.persistence;

import com.syncflow.model.Stage;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.repository.StageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StagePersistenceAdapter implements StageRepositoryPort {

    private final StageRepository stageRepository;

    @Override
    public List<Stage> findByProjectIdOrderByPositionAsc(Long projectId) {
        return stageRepository.findByProjectIdOrderByPositionAsc(projectId);
    }

    @Override
    public Optional<Stage> findById(Long id) {
        return stageRepository.findById(id);
    }

    @Override
    public Stage save(Stage stage) {
        return stageRepository.save(stage);
    }
}
