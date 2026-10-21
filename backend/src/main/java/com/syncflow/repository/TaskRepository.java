package com.syncflow.repository;

import com.syncflow.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdAndStageId(Long projectId, Long stageId);
    List<Task> findByAssigneeId(UUID assigneeId);
    long countByProjectId(Long projectId);
    long countByProjectIdAndIsBlockedTrue(Long projectId);
}
