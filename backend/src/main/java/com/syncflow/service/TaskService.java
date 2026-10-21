package com.syncflow.service;

import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskDTO;
import com.syncflow.model.Project;
import com.syncflow.model.Stage;
import com.syncflow.model.Task;
import com.syncflow.model.User;
import com.syncflow.repository.ProjectRepository;
import com.syncflow.repository.StageRepository;
import com.syncflow.repository.TaskRepository;
import com.syncflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskDTO createTask(CreateTaskRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        Stage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + request.getStageId()));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId()).orElse(null);
        }

        Task task = Task.builder()
                .project(project)
                .stage(stage)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .progressPercentage(0)
                .assignee(assignee)
                .isBlocked(false)
                .build();

        Task saved = taskRepository.save(task);
        log.info("Created Task ID: {} in Project: {}", saved.getId(), project.getName());
        return toDTO(saved);
    }

    @Transactional
    public TaskDTO moveTask(Long taskId, MoveTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        Stage targetStage = stageRepository.findById(request.getTargetStageId())
                .orElseThrow(() -> new IllegalArgumentException("Stage not found: " + request.getTargetStageId()));

        task.setStage(targetStage);
        if (Boolean.TRUE.equals(targetStage.getIsDoneStage())) {
            task.setProgressPercentage(100);
            task.setIsBlocked(false);
            task.setBlockerReason(null);
        }

        Task saved = taskRepository.save(task);
        log.info("Moved Task ID: {} to Stage: {}", saved.getId(), targetStage.getName());
        return toDTO(saved);
    }

    @Transactional
    public TaskDTO updateProgress(Long taskId, Integer progress) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setProgressPercentage(Math.max(0, Math.min(100, progress)));
        Task saved = taskRepository.save(task);
        return toDTO(saved);
    }

    @Transactional
    public TaskDTO setBlocker(Long taskId, SetBlockerRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setIsBlocked(request.getIsBlocked());
        if (Boolean.TRUE.equals(request.getIsBlocked())) {
            task.setBlockerReason(request.getBlockerReason());
            task.setBlockedAt(Instant.now());
        } else {
            task.setBlockerReason(null);
            task.setBlockedAt(null);
        }

        Task saved = taskRepository.save(task);
        log.info("Updated Blocker for Task ID: {} -> Blocked: {}", saved.getId(), saved.getIsBlocked());
        return toDTO(saved);
    }

    public TaskDTO toDTO(Task task) {
        String assigneeName = task.getAssignee() != null ? task.getAssignee().getFullName() : "Unassigned";
        String avatar = getInitials(assigneeName);

        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .progress(task.getProgressPercentage() != null ? task.getProgressPercentage() : 0)
                .stageId(task.getStage() != null ? task.getStage().getId() : null)
                .stageName(task.getStage() != null ? task.getStage().getName() : "")
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : "")
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assignee(assigneeName)
                .avatar(avatar)
                .isBlocked(Boolean.TRUE.equals(task.getIsBlocked()))
                .blockerReason(task.getBlockerReason())
                .build();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "UN";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }
}
