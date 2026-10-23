package com.syncflow.task.infrastructure.persistence;

import com.syncflow.model.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SpringDataSubtaskRepository extends JpaRepository<Subtask, Long> {
    List<Subtask> findByTaskIdOrderByPositionAsc(Long taskId);
    long countByTaskId(Long taskId);
    long countByTaskIdAndIsCompletedTrue(Long taskId);

    /** Rows of [taskId, total, completed]. */
    @Query("""
            SELECT s.task.id, COUNT(s), SUM(CASE WHEN s.isCompleted = true THEN 1 ELSE 0 END)
            FROM Subtask s WHERE s.task.id IN :taskIds GROUP BY s.task.id
            """)
    List<Object[]> countByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
