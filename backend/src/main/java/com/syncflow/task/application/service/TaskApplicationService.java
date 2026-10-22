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
    private final com.syncflow.repository.TaskDependencyRepository taskDependencyRepository;
    private final com.syncflow.repository.SprintRepository sprintRepository;
    private final com.syncflow.repository.AutomationRuleRepository automationRuleRepository;
    private final com.syncflow.service.WebhookDispatcherService webhookDispatcherService;
    private final com.syncflow.repository.TaskTimeLogRepository taskTimeLogRepository;

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

        try {
            webhookDispatcherService.dispatchEvent(saved.getProject().getId(), "TASK_CREATED", saved);
        } catch (Exception e) {
            log.warn("Failed to dispatch TASK_CREATED webhook: {}", e.getMessage());
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

        // Workflow Policy & Quality Gate Check
        com.syncflow.dto.WorkflowGateValidationResultDTO gate = validateTransitionGate(id, request.getTargetStageId());
        if (Boolean.TRUE.equals(gate.getRequiresOverride()) && !Boolean.TRUE.equals(request.getOverrideGate())) {
            throw new DomainException("WORKFLOW_GATE_VIOLATION: " + String.join("; ", gate.getViolations()));
        }

        if (Boolean.TRUE.equals(gate.getRequiresOverride()) && Boolean.TRUE.equals(request.getOverrideGate())) {
            String reason = request.getOverrideReason() != null && !request.getOverrideReason().isBlank()
                    ? request.getOverrideReason()
                    : "Manager override with elevated privilege";
            try {
                auditLogUseCase.recordLog("WORKFLOW_GATE_OVERRIDE", "TASK", id, task.getTitle(), "Manager", null, "MANAGER", reason);
            } catch (Exception e) {
                log.warn("Failed to record workflow gate override audit log: {}", e.getMessage());
            }
        }

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

        try {
            webhookDispatcherService.dispatchEvent(saved.getProject().getId(), "TASK_MOVED", saved);
        } catch (Exception e) {
            log.warn("Failed to dispatch TASK_MOVED webhook: {}", e.getMessage());
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
            try {
                webhookDispatcherService.dispatchEvent(saved.getProject().getId(), "TASK_BLOCKED", saved);
            } catch (Exception e) {
                log.warn("Failed to dispatch TASK_BLOCKED webhook: {}", e.getMessage());
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

    // --- Archiving & Lifecycle ---
    @Override
    @Transactional
    public TaskDTO archiveTask(Long id, String operatorName) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        task.setIsArchived(true);
        task.setArchivedAt(Instant.now());
        task.setArchivedBy(operatorName != null ? operatorName : "Operator");

        Task saved = taskRepositoryPort.save(task);
        log.info("Task {} archived by {}", id, operatorName);

        try {
            auditLogUseCase.recordLog("TASK_ARCHIVED", "TASK", saved.getId(), saved.getTitle(),
                    operatorName != null ? operatorName : "Operator", null, "COORDINATOR",
                    "Task archived from active Kanban board");
        } catch (Exception e) {
            log.warn("Failed to audit task archive: {}", e.getMessage());
        }

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO restoreTask(Long id, String operatorName) {
        Task task = taskRepositoryPort.findById(id)
                .orElseThrow(() -> new DomainException("Task not found: " + id));

        task.setIsArchived(false);
        task.setArchivedAt(null);
        task.setArchivedBy(null);

        Task saved = taskRepositoryPort.save(task);
        log.info("Task {} restored to board by {}", id, operatorName);

        try {
            auditLogUseCase.recordLog("TASK_RESTORED", "TASK", saved.getId(), saved.getTitle(),
                    operatorName != null ? operatorName : "Operator", null, "COORDINATOR",
                    "Task restored to active Kanban board");
        } catch (Exception e) {
            log.warn("Failed to audit task restore: {}", e.getMessage());
        }

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getArchivedTasks(Long projectId) {
        return taskRepositoryPort.findByProjectId(projectId).stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsArchived()))
                .map(this::toDTO)
                .toList();
    }

    // --- Task Dependencies & Blocker Links ---
    @Override
    @Transactional(readOnly = true)
    public List<com.syncflow.dto.TaskDependencyDTO> getDependencies(Long taskId) {
        List<com.syncflow.model.TaskDependency> outgoing = taskDependencyRepository.findBySourceTaskId(taskId);
        List<com.syncflow.model.TaskDependency> incoming = taskDependencyRepository.findByTargetTaskId(taskId);

        List<com.syncflow.dto.TaskDependencyDTO> list = new java.util.ArrayList<>();

        for (com.syncflow.model.TaskDependency dep : outgoing) {
            Task target = taskRepositoryPort.findById(dep.getTargetTaskId()).orElse(null);
            String title = target != null ? target.getTitle() : dep.getTargetTaskTitle();
            String priority = target != null ? target.getPriority() : "MEDIUM";
            int progress = target != null && target.getProgressPercentage() != null ? target.getProgressPercentage() : 0;
            String stage = target != null && target.getStage() != null ? target.getStage().getName() : "";
            String assignee = target != null && target.getAssignee() != null ? target.getAssignee().getFullName() : "Unassigned";
            boolean isBlocked = target != null && Boolean.TRUE.equals(target.getIsBlocked());

            list.add(com.syncflow.dto.TaskDependencyDTO.builder()
                    .id(dep.getId())
                    .sourceTaskId(dep.getSourceTaskId())
                    .targetTaskId(dep.getTargetTaskId())
                    .targetTaskTitle(title)
                    .targetTaskPriority(priority)
                    .targetTaskProgress(progress)
                    .targetTaskStageName(stage)
                    .targetTaskAssignee(assignee)
                    .dependencyType(dep.getDependencyType())
                    .isBlocked(isBlocked)
                    .createdBy(dep.getCreatedBy())
                    .createdAt(dep.getCreatedAt())
                    .build());
        }

        for (com.syncflow.model.TaskDependency dep : incoming) {
            Task source = taskRepositoryPort.findById(dep.getSourceTaskId()).orElse(null);
            String title = source != null ? source.getTitle() : "Task #" + dep.getSourceTaskId();
            String priority = source != null ? source.getPriority() : "MEDIUM";
            int progress = source != null && source.getProgressPercentage() != null ? source.getProgressPercentage() : 0;
            String stage = source != null && source.getStage() != null ? source.getStage().getName() : "";
            String assignee = source != null && source.getAssignee() != null ? source.getAssignee().getFullName() : "Unassigned";

            String invertedType = "BLOCKS".equalsIgnoreCase(dep.getDependencyType()) ? "BLOCKED_BY" :
                    ("BLOCKED_BY".equalsIgnoreCase(dep.getDependencyType()) ? "BLOCKS" : "RELATES_TO");

            list.add(com.syncflow.dto.TaskDependencyDTO.builder()
                    .id(dep.getId())
                    .sourceTaskId(taskId)
                    .targetTaskId(dep.getSourceTaskId())
                    .targetTaskTitle(title)
                    .targetTaskPriority(priority)
                    .targetTaskProgress(progress)
                    .targetTaskStageName(stage)
                    .targetTaskAssignee(assignee)
                    .dependencyType(invertedType)
                    .isBlocked(source != null && Boolean.TRUE.equals(source.getIsBlocked()))
                    .createdBy(dep.getCreatedBy())
                    .createdAt(dep.getCreatedAt())
                    .build());
        }

        return list;
    }

    @Override
    @Transactional
    public com.syncflow.dto.TaskDependencyDTO addDependency(Long sourceTaskId, Long targetTaskId, String type, String operatorName) {
        if (sourceTaskId.equals(targetTaskId)) {
            throw new DomainException("Task cannot depend on itself");
        }

        Task source = taskRepositoryPort.findById(sourceTaskId)
                .orElseThrow(() -> new DomainException("Source task not found: " + sourceTaskId));
        Task target = taskRepositoryPort.findById(targetTaskId)
                .orElseThrow(() -> new DomainException("Target task not found: " + targetTaskId));

        com.syncflow.model.TaskDependency dep = com.syncflow.model.TaskDependency.builder()
                .sourceTaskId(sourceTaskId)
                .targetTaskId(targetTaskId)
                .targetTaskTitle(target.getTitle())
                .dependencyType(type != null ? type.toUpperCase() : "BLOCKS")
                .createdBy(operatorName != null ? operatorName : "Operator")
                .createdAt(Instant.now())
                .build();

        com.syncflow.model.TaskDependency saved = taskDependencyRepository.save(dep);

        try {
            auditLogUseCase.recordLog("TASK_DEPENDENCY_LINKED", "TASK", sourceTaskId, source.getTitle(),
                    operatorName != null ? operatorName : "Operator", null, "COORDINATOR",
                    String.format("Linked dependency '%s' to target task #%d: %s", type, targetTaskId, target.getTitle()));
        } catch (Exception e) {
            log.warn("Failed to audit dependency link: {}", e.getMessage());
        }

        return com.syncflow.dto.TaskDependencyDTO.builder()
                .id(saved.getId())
                .sourceTaskId(sourceTaskId)
                .targetTaskId(targetTaskId)
                .targetTaskTitle(target.getTitle())
                .targetTaskPriority(target.getPriority())
                .targetTaskProgress(target.getProgressPercentage() != null ? target.getProgressPercentage() : 0)
                .targetTaskStageName(target.getStage() != null ? target.getStage().getName() : "")
                .targetTaskAssignee(target.getAssignee() != null ? target.getAssignee().getFullName() : "Unassigned")
                .dependencyType(saved.getDependencyType())
                .isBlocked(Boolean.TRUE.equals(target.getIsBlocked()))
                .createdBy(saved.getCreatedBy())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteDependency(Long dependencyId) {
        taskDependencyRepository.deleteById(dependencyId);
    }

    // --- Sprint & Story Points ---
    @Override
    @Transactional
    public TaskDTO updateStoryPoints(Long taskId, Integer points) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        task.setStoryPoints(points != null ? points : 3);
        Task saved = taskRepositoryPort.save(task);

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO assignSprint(Long taskId, Long sprintId) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        task.setSprintId(sprintId);
        Task saved = taskRepositoryPort.save(task);

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public com.syncflow.dto.WorkflowGateValidationResultDTO validateTransitionGate(Long taskId, Long targetStageId) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        Stage targetStage = stageRepositoryPort.findById(targetStageId)
                .orElseThrow(() -> new DomainException("Target stage not found: " + targetStageId));

        String stageName = targetStage.getName() != null ? targetStage.getName().toLowerCase() : "";
        List<String> violations = new java.util.ArrayList<>();
        List<String> blockingTaskTitles = new java.util.ArrayList<>();
        int uncompletedSubtasks = 0;
        boolean hasDeliverable = false;

        // Gate 1: Review / QA requires 100% Checklist Completion
        if (stageName.contains("review") || stageName.contains("qa")) {
            List<com.syncflow.model.Subtask> subtasks = subtaskRepository.findByTaskIdOrderByPositionAsc(taskId);
            for (com.syncflow.model.Subtask st : subtasks) {
                if (!Boolean.TRUE.equals(st.getIsCompleted())) {
                    uncompletedSubtasks++;
                }
            }
            if (uncompletedSubtasks > 0) {
                violations.add("Review Gate Policy: " + uncompletedSubtasks + " subtask(s) or QA checklist items remain uncompleted. 100% checklist completion is required.");
            }
        }

        // Gate 2: Done requires zero blocking dependencies and at least 1 deliverable / PR link
        if (stageName.contains("done") || Boolean.TRUE.equals(targetStage.getIsDoneStage())) {
            // Check direct blocker flag
            if (Boolean.TRUE.equals(task.getIsBlocked())) {
                violations.add("Done Gate Policy: Task is currently flagged as blocked: " + (task.getBlockerReason() != null ? task.getBlockerReason() : "Unspecified blocker"));
            }

            // Check outgoing dependency links (this task is blocked by target task)
            List<com.syncflow.model.TaskDependency> dependencies = taskDependencyRepository.findBySourceTaskId(taskId);
            for (com.syncflow.model.TaskDependency dep : dependencies) {
                if ("BLOCKED_BY".equalsIgnoreCase(dep.getDependencyType())) {
                    Task blocker = taskRepositoryPort.findById(dep.getTargetTaskId()).orElse(null);
                    if (blocker != null && (blocker.getStage() == null || !Boolean.TRUE.equals(blocker.getStage().getIsDoneStage()))) {
                        blockingTaskTitles.add(blocker.getTitle());
                    }
                }
            }

            // Check incoming dependency links (source task blocks this task)
            List<com.syncflow.model.TaskDependency> incoming = taskDependencyRepository.findByTargetTaskId(taskId);
            for (com.syncflow.model.TaskDependency dep : incoming) {
                if ("BLOCKS".equalsIgnoreCase(dep.getDependencyType())) {
                    Task blocker = taskRepositoryPort.findById(dep.getSourceTaskId()).orElse(null);
                    if (blocker != null && (blocker.getStage() == null || !Boolean.TRUE.equals(blocker.getStage().getIsDoneStage()))) {
                        blockingTaskTitles.add(blocker.getTitle());
                    }
                }
            }

            if (!blockingTaskTitles.isEmpty()) {
                violations.add("Done Gate Policy: Unresolved blocker tasks: " + String.join(", ", blockingTaskTitles));
            }

            // Check deliverable attachment or PR link
            List<com.syncflow.model.TaskAttachment> attachments = attachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId);
            hasDeliverable = (!attachments.isEmpty()) || (task.getGitPrUrl() != null && !task.getGitPrUrl().isBlank());
            if (!hasDeliverable) {
                violations.add("Done Gate Policy: Verification requires at least 1 deliverable attachment or linked Pull Request URL.");
            }
        }

        boolean requiresOverride = !violations.isEmpty();
        return com.syncflow.dto.WorkflowGateValidationResultDTO.builder()
                .allowed(!requiresOverride)
                .targetStageName(targetStage.getName())
                .requiresOverride(requiresOverride)
                .violations(violations)
                .blockingTaskTitles(blockingTaskTitles)
                .uncompletedSubtasksCount(uncompletedSubtasks)
                .hasDeliverableAttachment(hasDeliverable)
                .build();
    }

    @Override
    @Transactional
    public TaskDTO linkGit(Long taskId, com.syncflow.dto.GitLinkRequest request) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        task.setGitBranch(request.getGitBranch());
        task.setGitCommitHash(request.getGitCommitHash());
        task.setGitPrUrl(request.getGitPrUrl());

        Task saved = taskRepositoryPort.save(task);
        log.info("Linked Git repository to task {}: branch={}, commit={}, pr={}", taskId, request.getGitBranch(), request.getGitCommitHash(), request.getGitPrUrl());

        TaskDTO dto = toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    public TaskDTO toDTO(Task task) {
        String assigneeName = task.getAssignee() != null ? task.getAssignee().getFullName() : "Unassigned";
        String avatar = getInitials(assigneeName);

        String sprintName = null;
        if (task.getSprintId() != null) {
            sprintName = sprintRepository.findById(task.getSprintId()).map(com.syncflow.model.Sprint::getName).orElse(null);
        }

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
                .isArchived(Boolean.TRUE.equals(task.getIsArchived()))
                .archivedAt(task.getArchivedAt())
                .archivedBy(task.getArchivedBy())
                .startDate(task.getStartDate())
                .sprintId(task.getSprintId())
                .sprintName(sprintName)
                .storyPoints(task.getStoryPoints() != null ? task.getStoryPoints() : 3)
                .gitBranch(task.getGitBranch())
                .gitCommitHash(task.getGitCommitHash())
                .gitPrUrl(task.getGitPrUrl())
                .estimatedHours(task.getEstimatedHours() != null ? task.getEstimatedHours() : 8.0)
                .totalHoursSpent(task.getTotalHoursSpent() != null ? task.getTotalHoursSpent() : 0.0)
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

    // --- Time Tracking & Work Logs ---
    @Override
    @Transactional
    public com.syncflow.dto.TaskTimeLogDTO logTime(Long taskId, com.syncflow.dto.LogTimeRequest request, String userEmail) {
        Task task = taskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new DomainException("Task not found: " + taskId));

        User user = null;
        if (userEmail != null) {
            user = userRepositoryPort.findByEmail(userEmail).orElse(null);
        }
        if (user == null && task.getAssignee() != null) {
            user = task.getAssignee();
        }
        if (user == null) {
            user = userRepositoryPort.findByEmail("admin@syncflow.bank")
                    .orElseThrow(() -> new DomainException("User context required to log time"));
        }

        com.syncflow.model.TaskTimeLog timeLog = com.syncflow.model.TaskTimeLog.builder()
                .task(task)
                .user(user)
                .hoursSpent(request.getHoursSpent())
                .description(request.getDescription())
                .loggedDate(request.getLoggedDate() != null ? request.getLoggedDate() : java.time.LocalDate.now())
                .createdAt(Instant.now())
                .build();

        com.syncflow.model.TaskTimeLog saved = taskTimeLogRepository.save(timeLog);

        Double total = taskTimeLogRepository.sumHoursByTaskId(taskId);
        task.setTotalHoursSpent(total != null ? total : request.getHoursSpent());

        if (request.getEstimatedHours() != null && request.getEstimatedHours() > 0) {
            task.setEstimatedHours(request.getEstimatedHours());
        }
        taskRepositoryPort.save(task);

        try {
            auditLogUseCase.recordLog(
                    "TIME_LOGGED",
                    "TASK",
                    taskId,
                    task.getTitle(),
                    user.getFullName(),
                    null,
                    "HOURS: " + request.getHoursSpent(),
                    "Logged " + request.getHoursSpent() + "h on task '" + task.getTitle() + "'. Note: " + request.getDescription()
            );
        } catch (Exception e) {
            log.warn("Failed to record time log audit: {}", e.getMessage());
        }

        return toTimeLogDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.syncflow.dto.TaskTimeLogDTO> getTaskTimeLogs(Long taskId) {
        return taskTimeLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toTimeLogDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTimeLog(Long logId, String userEmail) {
        com.syncflow.model.TaskTimeLog logEntry = taskTimeLogRepository.findById(logId)
                .orElseThrow(() -> new DomainException("Time log not found: " + logId));

        Task task = logEntry.getTask();
        taskTimeLogRepository.delete(logEntry);

        Double total = taskTimeLogRepository.sumHoursByTaskId(task.getId());
        task.setTotalHoursSpent(total != null ? total : 0.0);
        taskRepositoryPort.save(task);
    }

    private com.syncflow.dto.TaskTimeLogDTO toTimeLogDTO(com.syncflow.model.TaskTimeLog logEntry) {
        return com.syncflow.dto.TaskTimeLogDTO.builder()
                .id(logEntry.getId())
                .taskId(logEntry.getTask() != null ? logEntry.getTask().getId() : null)
                .taskTitle(logEntry.getTask() != null ? logEntry.getTask().getTitle() : null)
                .userId(logEntry.getUser() != null ? logEntry.getUser().getId() : null)
                .userName(logEntry.getUser() != null ? logEntry.getUser().getFullName() : "Unknown")
                .userEmail(logEntry.getUser() != null ? logEntry.getUser().getEmail() : "")
                .userRole(logEntry.getUser() != null && logEntry.getUser().getRole() != null ? logEntry.getUser().getRole().getName() : "CONTRIBUTOR")
                .hoursSpent(logEntry.getHoursSpent())
                .description(logEntry.getDescription())
                .loggedDate(logEntry.getLoggedDate())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
