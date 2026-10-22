package com.syncflow.repository;

import com.syncflow.model.TaskTimeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskTimeLogRepository extends JpaRepository<TaskTimeLog, Long> {

    List<TaskTimeLog> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<TaskTimeLog> findByTaskProjectIdOrderByCreatedAtDesc(Long projectId);

    List<TaskTimeLog> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT COALESCE(SUM(t.hoursSpent), 0.0) FROM TaskTimeLog t WHERE t.task.id = :taskId")
    Double sumHoursByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COALESCE(SUM(t.hoursSpent), 0.0) FROM TaskTimeLog t WHERE t.task.project.id = :projectId")
    Double sumHoursByProjectId(@Param("projectId") Long projectId);
}
