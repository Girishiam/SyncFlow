package com.syncflow.repository;

import com.syncflow.model.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {
    List<TaskDependency> findBySourceTaskId(Long sourceTaskId);
    List<TaskDependency> findByTargetTaskId(Long targetTaskId);
    void deleteBySourceTaskIdAndTargetTaskId(Long sourceTaskId, Long targetTaskId);

    List<TaskDependency> findBySourceTaskIdIn(Collection<Long> sourceTaskIds);

    List<TaskDependency> findByTargetTaskIdIn(Collection<Long> targetTaskIds);

    boolean existsBySourceTaskIdAndTargetTaskId(Long sourceTaskId, Long targetTaskId);
}
