package com.syncflow.task.infrastructure.persistence;

import com.syncflow.model.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataSubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTaskIdOrderByPositionAsc(Long taskId);
    long countByTaskId(Long taskId);
    long countByTaskIdAndIsCompletedTrue(Long taskId);
}
