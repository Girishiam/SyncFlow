package com.syncflow.repository;

import com.syncflow.model.Project;
import com.syncflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByStatus(String status);
    List<Project> findAllByOrderByCreatedAtDesc();

    /** Project members and the project manager can access a project (admins are handled by the caller). */
    @Query("""
            SELECT COUNT(p) > 0 FROM Project p
            WHERE p.id = :projectId
              AND (p.manager.id = :userId
                   OR EXISTS (SELECT 1 FROM ProjectMember m WHERE m.projectId = p.id AND m.user.id = :userId))
            """)
    boolean isAccessibleBy(@Param("projectId") Long projectId, @Param("userId") UUID userId);

    @Query("""
            SELECT p FROM Project p
            WHERE p.manager.id = :userId
               OR EXISTS (SELECT 1 FROM ProjectMember m WHERE m.projectId = p.id AND m.user.id = :userId)
            ORDER BY p.id
            """)
    List<Project> findAccessibleBy(@Param("userId") UUID userId);

    /** The project team: every member plus the project manager. */
    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.id IN (SELECT m.user.id FROM ProjectMember m WHERE m.projectId = :projectId)
               OR u.id IN (SELECT p.manager.id FROM Project p WHERE p.id = :projectId)
            """)
    List<User> findTeamMembers(@Param("projectId") Long projectId);
}
