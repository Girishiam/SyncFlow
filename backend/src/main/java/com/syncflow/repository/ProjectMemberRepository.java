package com.syncflow.repository;

import com.syncflow.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProjectIdOrderByAddedAtAsc(Long projectId);
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, UUID userId);
    boolean existsByProjectIdAndUserId(Long projectId, UUID userId);
    long countByProjectId(Long projectId);
}
