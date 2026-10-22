package com.syncflow.repository;

import com.syncflow.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByStageId(Long stageId);
    List<Task> findByProjectIdAndStageId(Long projectId, Long stageId);
    List<Task> findByAssigneeId(UUID assigneeId);
    List<Task> findByProjectIdAndIsArchivedTrue(Long projectId);
    List<Task> findByProjectIdAndSprintId(Long projectId, Long sprintId);
    List<Task> findByProjectIdAndSprintIdIsNull(Long projectId);
    long countByProjectId(Long projectId);
    long countByProjectIdAndIsBlockedTrue(Long projectId);
}
