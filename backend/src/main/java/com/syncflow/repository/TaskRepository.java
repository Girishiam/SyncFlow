package com.syncflow.repository;

import com.syncflow.model.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"stage", "assignee"})
    List<Task> findByProjectId(Long projectId);

    /** Active (non-archived) tasks with the associations needed to render the board. */
    @EntityGraph(attributePaths = {"stage", "assignee"})
    List<Task> findByProjectIdAndIsArchivedFalse(Long projectId);

    List<Task> findByStageId(Long stageId);
    List<Task> findByProjectIdAndStageId(Long projectId, Long stageId);
    List<Task> findByAssigneeId(UUID assigneeId);
    List<Task> findByProjectIdAndIsArchivedTrue(Long projectId);
    List<Task> findByProjectIdAndSprintId(Long projectId, Long sprintId);
    List<Task> findBySprintId(Long sprintId);
    List<Task> findByProjectIdAndSprintIdIsNull(Long projectId);
    long countByProjectId(Long projectId);
    long countByProjectIdAndIsBlockedTrue(Long projectId);
    boolean existsByStageId(Long stageId);

    @Query("SELECT t.project.id FROM Task t WHERE t.id = :taskId")
    Optional<Long> findProjectIdById(@Param("taskId") Long taskId);

    /** Open, assigned, non-archived tasks whose deadline falls before {@code until}. */
    @EntityGraph(attributePaths = {"stage", "assignee"})
    @Query("""
            SELECT t FROM Task t JOIN t.stage s
            WHERE t.isArchived = false
              AND t.assignee IS NOT NULL
              AND t.deadline IS NOT NULL
              AND t.deadline < :until
              AND s.isDoneStage = false
            """)
    List<Task> findOpenAssignedWithDeadlineBefore(@Param("until") Instant until);
}
