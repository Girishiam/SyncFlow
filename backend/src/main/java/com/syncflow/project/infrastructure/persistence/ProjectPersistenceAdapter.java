package com.syncflow.project.infrastructure.persistence;

import com.syncflow.model.ProjectMember;
import com.syncflow.model.User;
import java.util.UUID;

import com.syncflow.model.Project;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.repository.ProjectMemberRepository;
import com.syncflow.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectPersistenceAdapter implements ProjectRepositoryPort {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    @Override
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public boolean isAccessibleBy(Long projectId, UUID userId) {
        return projectRepository.isAccessibleBy(projectId, userId);
    }

    @Override
    public List<Project> findAccessibleBy(UUID userId) {
        return projectRepository.findAccessibleBy(userId);
    }

    @Override
    public List<User> findTeamMembers(Long projectId) {
        return projectRepository.findTeamMembers(projectId);
    }

    @Override
    public List<ProjectMember> findMembers(Long projectId) {
        return projectMemberRepository.findByProjectIdOrderByAddedAtAsc(projectId);
    }

    @Override
    public Optional<ProjectMember> findMember(Long projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public ProjectMember saveMember(ProjectMember member) {
        return projectMemberRepository.save(member);
    }

    @Override
    public void deleteMember(ProjectMember member) {
        projectMemberRepository.delete(member);
    }

    @Override
    public long countMembers(Long projectId) {
        return projectMemberRepository.countByProjectId(projectId);
    }
}
