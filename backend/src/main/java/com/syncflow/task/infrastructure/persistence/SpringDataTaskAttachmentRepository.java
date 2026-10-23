package com.syncflow.task.infrastructure.persistence;

import com.syncflow.model.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SpringDataTaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    List<TaskAttachment> findByTaskIdOrderByUploadedAtDesc(Long taskId);

    /** Rows of [taskId, count]. */
    @Query("SELECT a.task.id, COUNT(a) FROM TaskAttachment a WHERE a.task.id IN :taskIds GROUP BY a.task.id")
    List<Object[]> countByTaskIds(@Param("taskIds") Collection<Long> taskIds);
}
