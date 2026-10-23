package com.syncflow.project.application.port.out;

import com.syncflow.model.Sprint;
import com.syncflow.model.enums.SprintStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Sprint}. */
public interface SprintRepositoryPort {
    Sprint save(Sprint sprint);
    Optional<Sprint> findById(Long id);
    List<Sprint> findAllById(Collection<Long> ids);
    List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId);
    Optional<Sprint> findFirstByProjectIdAndStatus(Long projectId, SprintStatus status);
    boolean existsByProjectIdAndStatus(Long projectId, SprintStatus status);
    List<Sprint> findTop3ByProjectIdAndStatusOrderByEndDateDesc(Long projectId, SprintStatus status);
}
