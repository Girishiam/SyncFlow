package com.syncflow.task.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskDTO;
import com.syncflow.model.Project;
import com.syncflow.model.Stage;
import com.syncflow.model.Task;
import com.syncflow.model.User;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.task.application.port.in.TaskUseCase;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskApplicationService implements TaskUseCase {

    private final TaskRepositoryPort taskRepositoryPort;
    private final ProjectRepositoryPort projectRepositoryPort;
    private final StageRepositoryPort stageRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TaskNotificationPort taskNotificationPort;

    @Override
    @Transactional
    public TaskDTO createTask(CreateTaskRequest request) {
        Project project = projectRepositoryPort.findById(request.getProjectId())
                .orElseThrow(() -> new DomainException("Project not found: " + request.getProjectId()));

        Stage stage = stageRepositoryPort.findById(request.getStageId())
                .orElseThrow(() -> new DomainException("Stage not found: " + request.getStageId()));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepositoryPort.findById(request.getAssigneeId()).orElse(null);
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

        Task saved = taskRepositoryPort.save(task);
        log.info("Task created successfully with ID: {}", saved.getId());

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO moveTask(Long id, MoveTaskRequest request) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        Stage targetStage = stageRepositoryPort.findById(request.getTargetStageId())
                .orElseThrow(() -> new DomainException("Target Stage not found: " + request.getTargetStageId()));

        task.setStage(targetStage);
        if (Boolean.TRUE.equals(targetStage.getIsDoneStage())) {
            task.setProgressPercentage(100);
            task.setIsBlocked(false);
            task.setBlockerReason(null);
        }

        Task saved = taskRepositoryPort.save(task);
        log.info("Task {} migrated to stage {}", saved.getId(), targetStage.getName());

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        task.setProgressPercentage(Math.max(0, Math.min(100, progress)));
        Task saved = taskRepositoryPort.save(task);

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO setBlocker(Long id, SetBlockerRequest request) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        task.setIsBlocked(request.getIsBlocked());
        if (Boolean.TRUE.equals(request.getIsBlocked())) {
            task.setBlockerReason(request.getBlockerReason());
            task.setBlockedAt(Instant.now());
        } else {
            task.setBlockerReason(null);
            task.setBlockedAt(null);
        }

        Task saved = taskRepositoryPort.save(task);
        TaskDTO dto = toDTO(saved);

        taskNotificationPort.broadcastBoardUpdate(dto);
        if (Boolean.TRUE.equals(saved.getIsBlocked())) {
            taskNotificationPort.broadcastBlockerAlert(dto);
        }
        return dto;
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
