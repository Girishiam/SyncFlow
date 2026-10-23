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

    /**
     * A project without a team is organisation-wide. Otherwise access is limited to the
     * project manager, the team lead and the team members.
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM Project p
            LEFT JOIN p.team t
            LEFT JOIN p.manager mg
            LEFT JOIN t.lead ld
            LEFT JOIN t.members m
            WHERE p.id = :projectId
              AND (t IS NULL OR mg.id = :userId OR ld.id = :userId OR m.id = :userId)
            """)
    boolean isAccessibleBy(@Param("projectId") Long projectId, @Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT p FROM Project p
            LEFT JOIN p.team t
            LEFT JOIN p.manager mg
            LEFT JOIN t.lead ld
            LEFT JOIN t.members m
            WHERE t IS NULL OR mg.id = :userId OR ld.id = :userId OR m.id = :userId
            ORDER BY p.id
            """)
    List<Project> findAccessibleBy(@Param("userId") UUID userId);

    /** Members of a project's team including its lead and the project manager. */
    @Query("""
            SELECT DISTINCT u FROM User u
            WHERE u.id IN (SELECT m.id FROM Project p JOIN p.team t JOIN t.members m WHERE p.id = :projectId)
               OR u.id IN (SELECT t.lead.id FROM Project p JOIN p.team t WHERE p.id = :projectId)
               OR u.id IN (SELECT p.manager.id FROM Project p WHERE p.id = :projectId)
            """)
    List<User> findTeamMembers(@Param("projectId") Long projectId);
}
