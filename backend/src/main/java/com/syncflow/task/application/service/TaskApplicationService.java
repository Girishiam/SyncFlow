package com.syncflow.task.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.AddCommentRequest;
import com.syncflow.dto.AssignTaskRequest;
import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskCommentDTO;
import com.syncflow.dto.TaskDTO;
import com.syncflow.dto.UpdateDeadlineRequest;
import com.syncflow.model.Project;
import com.syncflow.model.Stage;
import com.syncflow.model.Task;
import com.syncflow.model.TaskComment;
import com.syncflow.model.User;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.task.application.port.in.TaskUseCase;
import com.syncflow.task.application.port.out.TaskCommentRepositoryPort;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskApplicationService implements TaskUseCase {

    private final TaskRepositoryPort taskRepositoryPort;
    private final ProjectRepositoryPort projectRepositoryPort;
    private final StageRepositoryPort stageRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TaskNotificationPort taskNotificationPort;
    private final TaskCommentRepositoryPort taskCommentRepositoryPort;
    private final com.syncflow.notification.application.port.in.NotificationUseCase notificationUseCase;
    private final com.syncflow.task.infrastructure.persistence.SpringDataSubtaskRepository subtaskRepository;
    private final com.syncflow.task.infrastructure.persistence.SpringDataTaskAttachmentRepository attachmentRepository;
    private final com.syncflow.audit.application.port.in.AuditLogUseCase auditLogUseCase;

    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskDetails(Long id) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));
        return toDTO(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentDTO> getTaskComments(Long taskId) {
        return taskCommentRepositoryPort.findByTaskId(taskId).stream()
                .map(this::toCommentDTO)
                .toList();
    }

    @Override
    @Transactional
    public TaskCommentDTO addComment(Long taskId, AddCommentRequest request, UUID authorId) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        User author = userRepositoryPort.findById(authorId)
                .orElseThrow(() -> new DomainException("User not found: " + authorId));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(author)
                .content(request.getContent().trim())
                .attachmentUrl(request.getAttachmentUrl())
                .build();

        TaskComment saved = taskCommentRepositoryPort.save(comment);
        log.info("Added comment to task {} by {}", taskId, author.getFullName());
        return toCommentDTO(saved);
    }

    private TaskCommentDTO toCommentDTO(TaskComment c) {
        String roleName = c.getAuthor() != null && c.getAuthor().getRole() != null
                ? c.getAuthor().getRole().getName()
                : "Member";
        String authorName = c.getAuthor() != null ? c.getAuthor().getFullName() : "User";

        return TaskCommentDTO.builder()
                .id(c.getId())
                .taskId(c.getTask() != null ? c.getTask().getId() : null)
                .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                .authorName(authorName)
                .authorRole(roleName)
                .authorAvatar(getInitials(authorName))
                .content(c.getContent())
                .time(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "Just now")
                .attachmentUrl(c.getAttachmentUrl())
                .build();
    }

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
                .deadline(request.getDeadline())
                .isBlocked(false)
                .build();

        Task saved = taskRepositoryPort.save(task);
        log.info("Task created successfully with ID: {}", saved.getId());

        if (saved.getAssignee() != null) {
            try {
                notificationUseCase.notifyTaskAssigned(saved, saved.getAssignee(), null);
            } catch (Exception e) {
                log.warn("Failed to dispatch assignment notification for task {}: {}", saved.getId(), e.getMessage());
            }
        }

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

        if (Boolean.TRUE.equals(targetStage.getIsDoneStage())) {
            try {
                notificationUseCase.notifyTaskCompleted(saved, saved.getAssignee());
            } catch (Exception e) {
                log.warn("Failed to dispatch task completion notification: {}", e.getMessage());
            }
        }

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        boolean wasDone = (task.getProgressPercentage() != null && task.getProgressPercentage() == 100);
        task.setProgressPercentage(Math.max(0, Math.min(100, progress)));
        if (task.getProgressPercentage() == 100) {
            task.setIsBlocked(false);
            task.setBlockerReason(null);
        }

        Task saved = taskRepositoryPort.save(task);

        if (!wasDone && saved.getProgressPercentage() == 100) {
            try {
                notificationUseCase.notifyTaskCompleted(saved, saved.getAssignee());
            } catch (Exception e) {
                log.warn("Failed to dispatch task completion notification: {}", e.getMessage());
            }
        }

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
            try {
                notificationUseCase.notifyBlockerAlert(saved, request.getBlockerReason(), null);
            } catch (Exception e) {
                log.warn("Failed to dispatch blocker notification for task {}: {}", saved.getId(), e.getMessage());
            }
        }
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO assignTask(Long id, AssignTaskRequest request) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepositoryPort.findById(request.getAssigneeId()).orElse(null);
        }

        task.setAssignee(assignee);
        Task saved = taskRepositoryPort.save(task);
        log.info("Task {} assignee updated to {}", id, assignee != null ? assignee.getFullName() : "Unassigned");

        if (saved.getAssignee() != null) {
            try {
                notificationUseCase.notifyTaskAssigned(saved, saved.getAssignee(), null);
            } catch (Exception e) {
                log.warn("Failed to dispatch reassignment notification for task {}: {}", saved.getId(), e.getMessage());
            }
        }

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO updateDeadline(Long id, UpdateDeadlineRequest request) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        task.setDeadline(request.getDeadline());
        Task saved = taskRepositoryPort.save(task);
        log.info("Task {} deadline updated to {}", id, request.getDeadline());

        try {
            auditLogUseCase.recordLog("TASK_DEADLINE_UPDATED", "TASK", saved.getId(), saved.getTitle(),
                    "System Operator", null, "COORDINATOR", "Updated deadline to " + request.getDeadline());
        } catch (Exception e) {
            log.warn("Failed to audit log deadline update: {}", e.getMessage());
        }

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    // --- Subtasks & Deliverable QA Checklist ---
    @Override
    @Transactional(readOnly = true)
    public List<com.syncflow.dto.SubtaskDTO> getSubtasks(Long taskId) {
        return subtaskRepository.findByTaskIdOrderByPositionAsc(taskId).stream()
                .map(s -> com.syncflow.dto.SubtaskDTO.builder()
                        .id(s.getId())
                        .taskId(s.getTask().getId())
                        .title(s.getTitle())
                        .isCompleted(s.getIsCompleted())
                        .completedBy(s.getCompletedBy())
                        .completedAt(s.getCompletedAt())
                        .position(s.getPosition())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public com.syncflow.dto.SubtaskDTO addSubtask(Long taskId, String title, String authorName) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        long count = subtaskRepository.countByTaskId(taskId);
        com.syncflow.model.Subtask subtask = com.syncflow.model.Subtask.builder()
                .task(task)
                .title(title.trim())
                .isCompleted(false)
                .position((int) count + 1)
                .createdAt(Instant.now())
                .build();

        com.syncflow.model.Subtask saved = subtaskRepository.save(subtask);

        try {
            auditLogUseCase.recordLog("TASK_SUBTASK_ADDED", "TASK", task.getId(), task.getTitle(),
                    authorName != null ? authorName : "Team Member", null, "ENGINEER",
                    "Added deliverable checklist item: " + title);
        } catch (Exception e) {
            log.warn("Failed to audit subtask creation: {}", e.getMessage());
        }

        return com.syncflow.dto.SubtaskDTO.builder()
                .id(saved.getId())
                .taskId(task.getId())
                .title(saved.getTitle())
                .isCompleted(saved.getIsCompleted())
                .position(saved.getPosition())
                .build();
    }

    @Override
    @Transactional
    public com.syncflow.dto.SubtaskDTO toggleSubtask(Long taskId, Long subtaskId, String actorName) {
        com.syncflow.model.Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new DomainException("Subtask not found: " + subtaskId));

        boolean newStatus = !Boolean.TRUE.equals(subtask.getIsCompleted());
        subtask.setIsCompleted(newStatus);
        if (newStatus) {
            subtask.setCompletedBy(actorName != null ? actorName : "Team Member");
            subtask.setCompletedAt(Instant.now());
        } else {
            subtask.setCompletedBy(null);
            subtask.setCompletedAt(null);
        }

        com.syncflow.model.Subtask saved = subtaskRepository.save(subtask);

        try {
            auditLogUseCase.recordLog(newStatus ? "TASK_SUBTASK_VERIFIED" : "TASK_SUBTASK_REOPENED",
                    "TASK", taskId, subtask.getTask().getTitle(),
                    actorName != null ? actorName : "QA Engineer", null, "QA_ENGINEER",
                    (newStatus ? "Verified checklist item: " : "Reopened checklist item: ") + subtask.getTitle());
        } catch (Exception e) {
            log.warn("Failed to audit subtask toggle: {}", e.getMessage());
        }

        return com.syncflow.dto.SubtaskDTO.builder()
                .id(saved.getId())
                .taskId(taskId)
                .title(saved.getTitle())
                .isCompleted(saved.getIsCompleted())
                .completedBy(saved.getCompletedBy())
                .completedAt(saved.getCompletedAt())
                .position(saved.getPosition())
                .build();
    }

    @Override
    @Transactional
    public void deleteSubtask(Long taskId, Long subtaskId) {
        subtaskRepository.deleteById(subtaskId);
    }

    // --- Artifacts & File Attachments ---
    @Override
    @Transactional(readOnly = true)
    public List<com.syncflow.dto.TaskAttachmentDTO> getAttachments(Long taskId) {
        return attachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId).stream()
                .map(a -> {
                    String timeAgo = "Just now";
                    if (a.getUploadedAt() != null) {
                        java.time.Duration d = java.time.Duration.between(a.getUploadedAt(), Instant.now());
                        if (d.toDays() > 0) timeAgo = d.toDays() + "d ago";
                        else if (d.toHours() > 0) timeAgo = d.toHours() + "h ago";
                        else if (d.toMinutes() > 0) timeAgo = d.toMinutes() + "m ago";
                    }
                    return com.syncflow.dto.TaskAttachmentDTO.builder()
                            .id(a.getId())
                            .taskId(a.getTask().getId())
                            .fileName(a.getFileName())
                            .fileType(a.getFileType())
                            .fileSize(a.getFileSize())
                            .fileUrl(a.getFileUrl())
                            .uploadedBy(a.getUploadedBy())
                            .uploadedAt(a.getUploadedAt())
                            .timeAgo(timeAgo)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public com.syncflow.dto.TaskAttachmentDTO addAttachment(Long taskId, String fileName, String fileType, String fileSize, String fileUrl, String uploadedBy) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        com.syncflow.model.TaskAttachment attachment = com.syncflow.model.TaskAttachment.builder()
                .task(task)
                .fileName(fileName)
                .fileType(fileType != null ? fileType : "document")
                .fileSize(fileSize != null ? fileSize : "1.2 MB")
                .fileUrl(fileUrl)
                .uploadedBy(uploadedBy != null ? uploadedBy : "Team Member")
                .uploadedAt(Instant.now())
                .build();

        com.syncflow.model.TaskAttachment saved = attachmentRepository.save(attachment);

        try {
            auditLogUseCase.recordLog("TASK_ATTACHMENT_ADDED", "TASK", task.getId(), task.getTitle(),
                    uploadedBy != null ? uploadedBy : "Engineer", null, "DEVELOPER",
                    "Uploaded deliverable attachment: " + fileName);
        } catch (Exception e) {
            log.warn("Failed to audit attachment upload: {}", e.getMessage());
        }

        return com.syncflow.dto.TaskAttachmentDTO.builder()
                .id(saved.getId())
                .taskId(task.getId())
                .fileName(saved.getFileName())
                .fileType(saved.getFileType())
                .fileSize(saved.getFileSize())
                .fileUrl(saved.getFileUrl())
                .uploadedBy(saved.getUploadedBy())
                .uploadedAt(saved.getUploadedAt())
                .timeAgo("Just now")
                .build();
    }

    @Override
    @Transactional
    public void deleteAttachment(Long taskId, Long attachmentId) {
        attachmentRepository.deleteById(attachmentId);
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
                .deadline(task.getDeadline())
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
