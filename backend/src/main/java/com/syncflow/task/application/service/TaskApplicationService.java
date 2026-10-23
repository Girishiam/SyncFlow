package com.syncflow.task.application.service;

import com.syncflow.project.application.port.out.SprintRepositoryPort;
import com.syncflow.task.application.port.out.TaskDependencyRepositoryPort;
import com.syncflow.task.application.port.out.TaskTimeLogRepositoryPort;
import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.application.port.out.TaskAttachmentRepositoryPort;
import com.syncflow.task.application.port.out.AttachmentStoragePort;

import com.syncflow.model.enums.*;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.automation.application.AutomationEngine;
import com.syncflow.common.domain.ConflictException;
import com.syncflow.common.domain.DomainException;
import com.syncflow.common.domain.ForbiddenException;
import com.syncflow.common.domain.NotFoundException;
import com.syncflow.common.domain.WorkflowGateViolationException;
import com.syncflow.common.event.ProjectEvent;
import com.syncflow.dto.*;
import com.syncflow.model.*;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.security.CurrentUser;
import com.syncflow.security.JwtUtils;
import com.syncflow.security.Permissions;
import com.syncflow.security.UserDetailsImpl;
import com.syncflow.task.application.port.in.TaskUseCase;
import com.syncflow.task.application.port.out.TaskCommentRepositoryPort;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskApplicationService implements TaskUseCase {

    public static final String GATE_OVERRIDE_SCOPE = "WORKFLOW_GATE_OVERRIDE";
    private static final int MIN_OVERRIDE_REASON_LENGTH = 10;

    private final TaskRepositoryPort taskRepositoryPort;
    private final ProjectRepositoryPort projectRepositoryPort;
    private final StageRepositoryPort stageRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final TaskNotificationPort taskNotificationPort;
    private final TaskCommentRepositoryPort taskCommentRepositoryPort;
    private final NotificationUseCase notificationUseCase;
    private final SubtaskRepositoryPort subtaskRepository;
    private final TaskAttachmentRepositoryPort attachmentRepository;
    private final AuditLogUseCase auditLogUseCase;
    private final TaskDependencyRepositoryPort taskDependencyRepository;
    private final SprintRepositoryPort sprintRepository;
    private final TaskTimeLogRepositoryPort taskTimeLogRepository;
    private final TaskMapper taskMapper;
    private final WorkflowGateService workflowGateService;
    private final AutomationEngine automationEngine;
    private final AttachmentStoragePort attachmentStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUser currentUser;
    private final JwtUtils jwtUtils;

    // ---------------------------------------------------------------- core task lifecycle

    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskDetails(Long id) {
        return taskMapper.toDTO(requireTask(id));
    }

    @Override
    @Transactional
    public TaskDTO createTask(CreateTaskRequest request) {
        Project project = projectRepositoryPort.findById(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found: " + request.getProjectId()));
        Stage stage = requireStageInProject(request.getStageId(), project.getId());
        User assignee = request.getAssigneeId() != null ? requireActiveUser(request.getAssigneeId()) : null;
        User actor = actorEntity();

        Task task = Task.builder()
                .project(project)
                .stage(stage)
                .title(requireText(request.getTitle(), "Title", 255))
                .description(request.getDescription())
                .priority(TaskPriority.parse(request.getPriority()))
                .progressPercentage(0)
                .assignee(assignee)
                .reporter(actor)
                .deadline(request.getDeadline())
                .isBlocked(false)
                .completedAt(Boolean.TRUE.equals(stage.getIsDoneStage()) ? Instant.now() : null)
                .build();

        Task saved = taskRepositoryPort.save(task);
        if (saved.getAssignee() != null) {
            notificationUseCase.notifyTaskAssigned(saved, saved.getAssignee(), actor);
        }
        auditLogUseCase.recordLog("TASK_CREATED", "TASK", saved.getId(), saved.getTitle(), null, null, null,
                "Created task with " + saved.getPriority() + " priority in " + project.getName());

        TaskDTO dto = taskMapper.toDTO(saved);
        eventPublisher.publishEvent(ProjectEvent.of(project.getId(), ProjectEvent.TASK_CREATED, dto));
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO moveTask(Long id, MoveTaskRequest request) {
        Task task = requireTask(id);
        Stage targetStage = requireStageInProject(request.getTargetStageId(), task.getProject().getId());
        if (task.getStage() != null && task.getStage().getId().equals(targetStage.getId())) {
            return taskMapper.toDTO(task);
        }

        WorkflowGateValidationResultDTO gate = workflowGateService.evaluate(task, targetStage);
        if (Boolean.TRUE.equals(gate.getRequiresOverride())) {
            if (!Boolean.TRUE.equals(request.getOverrideGate())) {
                throw new WorkflowGateViolationException(gate.getViolations());
            }
            authorizeGateOverride(request);
            auditLogUseCase.recordLog("WORKFLOW_GATE_OVERRIDE", "TASK", id, task.getTitle(), null, null, null,
                    "Overrode gates for '" + targetStage.getName() + "'. Reason: " + request.getOverrideReason().trim()
                            + " | Violations: " + String.join("; ", gate.getViolations()));
        }

        boolean wasDone = TaskMapper.isDone(task);
        boolean nowDone = Boolean.TRUE.equals(targetStage.getIsDoneStage());
        String fromStage = task.getStage() != null ? task.getStage().getName() : "none";

        task.setStage(targetStage);
        if (nowDone) {
            task.setProgressPercentage(100);
            task.setIsBlocked(false);
            task.setBlockerReason(null);
            if (!wasDone) task.setCompletedAt(Instant.now());
        } else if (wasDone) {
            task.setCompletedAt(null);
        }

        Task saved = taskRepositoryPort.save(task);
        auditLogUseCase.recordLog("TASK_MOVED", "TASK", saved.getId(), saved.getTitle(), null, null, null,
                "Moved from " + fromStage + " to " + targetStage.getName());

        if (nowDone && !wasDone) {
            notificationUseCase.notifyTaskCompleted(saved, actorEntity());
        }
        automationEngine.onTaskMoved(saved, targetStage);

        TaskDTO dto = taskMapper.toDTO(saved);
        eventPublisher.publishEvent(ProjectEvent.of(saved.getProject().getId(), ProjectEvent.TASK_MOVED, dto));
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    /**
     * Overriding a failed gate requires the dedicated permission, a fresh password re-authentication
     * (elevation token) for the same user, and a written justification.
     */
    private void authorizeGateOverride(MoveTaskRequest request) {
        UserDetailsImpl user = currentUser.require();
        if (!CurrentUser.has(user, Permissions.TASK_OVERRIDE_GATE)) {
            throw new ForbiddenException("Your role cannot override workflow gates");
        }
        if (!jwtUtils.isValidElevation(request.getElevationToken(), user.getEmail(), GATE_OVERRIDE_SCOPE)) {
            throw new ForbiddenException("Confirm your password to override workflow gates (elevation missing or expired)");
        }
        if (request.getOverrideReason() == null || request.getOverrideReason().trim().length() < MIN_OVERRIDE_REASON_LENGTH) {
            throw new DomainException("An override reason of at least " + MIN_OVERRIDE_REASON_LENGTH + " characters is required");
        }
    }

    @Override
    @Transactional
    public TaskDTO updateProgress(Long id, Integer progress) {
        Task task = requireTask(id);
        task.setProgressPercentage(Math.max(0, Math.min(100, progress)));
        if (task.getProgressPercentage() == 100) {
            task.setIsBlocked(false);
            task.setBlockerReason(null);
        }
        return saveAndBroadcast(task);
    }

    @Override
    @Transactional
    public TaskDTO setBlocker(Long id, SetBlockerRequest request) {
        Task task = requireTask(id);
        boolean blocking = Boolean.TRUE.equals(request.getIsBlocked());
        if (blocking && (request.getBlockerReason() == null || request.getBlockerReason().isBlank())) {
            throw new DomainException("A blocker reason is required");
        }

        task.setIsBlocked(blocking);
        task.setBlockerReason(blocking ? request.getBlockerReason().trim() : null);
        task.setBlockedAt(blocking ? Instant.now() : null);
        Task saved = taskRepositoryPort.save(task);
        TaskDTO dto = taskMapper.toDTO(saved);

        auditLogUseCase.recordLog(blocking ? "TASK_BLOCKED" : "TASK_UNBLOCKED", "TASK", saved.getId(), saved.getTitle(),
                null, null, null, blocking ? "Blocker: " + saved.getBlockerReason() : "Blocker cleared");

        taskNotificationPort.broadcastBoardUpdate(dto);
        if (blocking) {
            taskNotificationPort.broadcastBlockerAlert(dto);
            notificationUseCase.notifyBlockerAlert(saved, saved.getBlockerReason(), actorEntity());
            automationEngine.onTaskBlocked(saved);
            eventPublisher.publishEvent(ProjectEvent.of(saved.getProject().getId(), ProjectEvent.TASK_BLOCKED, dto));
        }
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO assignTask(Long id, AssignTaskRequest request) {
        Task task = requireTask(id);
        User assignee = request.getAssigneeId() != null ? requireActiveUser(request.getAssigneeId()) : null;
        task.setAssignee(assignee);
        Task saved = taskRepositoryPort.save(task);

        auditLogUseCase.recordLog("TASK_ASSIGNED", "TASK", saved.getId(), saved.getTitle(), null, null, null,
                "Assigned to " + (assignee != null ? assignee.getFullName() : "nobody"));
        if (assignee != null) {
            notificationUseCase.notifyTaskAssigned(saved, assignee, actorEntity());
        }
        TaskDTO dto = taskMapper.toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    @Override
    @Transactional
    public TaskDTO updateDeadline(Long id, UpdateDeadlineRequest request) {
        Task task = requireTask(id);
        task.setDeadline(request.getDeadline());
        Task saved = taskRepositoryPort.save(task);
        auditLogUseCase.recordLog("TASK_DEADLINE_UPDATED", "TASK", saved.getId(), saved.getTitle(), null, null, null,
                "Deadline set to " + (request.getDeadline() != null ? request.getDeadline() : "none"));
        TaskDTO dto = taskMapper.toDTO(saved);
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    // ---------------------------------------------------------------- comments

    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentDTO> getTaskComments(Long taskId) {
        return taskCommentRepositoryPort.findByTaskId(taskId).stream()
                .map(this::toCommentDTO)
                .toList();
    }

    @Override
    @Transactional
    public TaskCommentDTO addComment(Long taskId, AddCommentRequest request) {
        Task task = requireTask(taskId);
        User author = actorEntity();
        if (author == null) {
            throw new ForbiddenException("An authenticated user is required to comment");
        }
        if (request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank()) {
            requireHttpUrl(request.getAttachmentUrl(), "Attachment URL");
        }

        TaskComment saved = taskCommentRepositoryPort.save(TaskComment.builder()
                .task(task)
                .author(author)
                .content(requireText(request.getContent(), "Comment", 4000))
                .attachmentUrl(request.getAttachmentUrl())
                .build());
        return toCommentDTO(saved);
    }

    private TaskCommentDTO toCommentDTO(TaskComment c) {
        String authorName = c.getAuthor() != null ? c.getAuthor().getFullName() : "User";
        return TaskCommentDTO.builder()
                .id(c.getId())
                .taskId(c.getTask() != null ? c.getTask().getId() : null)
                .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                .authorName(authorName)
                .authorRole(c.getAuthor() != null && c.getAuthor().getRole() != null ? c.getAuthor().getRole().getName() : "Member")
                .authorAvatar(TaskMapper.initials(authorName))
                .content(c.getContent())
                .time(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .attachmentUrl(c.getAttachmentUrl())
                .build();
    }

    // ---------------------------------------------------------------- checklist

    @Override
    @Transactional(readOnly = true)
    public List<SubtaskDTO> getSubtasks(Long taskId) {
        return subtaskRepository.findByTaskIdOrderByPositionAsc(taskId).stream()
                .map(this::toSubtaskDTO)
                .toList();
    }

    @Override
    @Transactional
    public SubtaskDTO addSubtask(Long taskId, String title) {
        Task task = requireTask(taskId);
        String cleanTitle = requireText(title, "Checklist item", 255);
        long count = subtaskRepository.countByTaskId(taskId);

        Subtask saved = subtaskRepository.save(Subtask.builder()
                .task(task)
                .title(cleanTitle)
                .isCompleted(false)
                .position((int) count + 1)
                .createdAt(Instant.now())
                .build());
        auditLogUseCase.recordLog("TASK_SUBTASK_ADDED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Added checklist item: " + cleanTitle);
        return toSubtaskDTO(saved);
    }

    @Override
    @Transactional
    public SubtaskDTO toggleSubtask(Long taskId, Long subtaskId) {
        Subtask subtask = requireSubtask(taskId, subtaskId);
        boolean completed = !Boolean.TRUE.equals(subtask.getIsCompleted());
        subtask.setIsCompleted(completed);
        subtask.setCompletedBy(completed ? actorName() : null);
        subtask.setCompletedAt(completed ? Instant.now() : null);
        Subtask saved = subtaskRepository.save(subtask);

        auditLogUseCase.recordLog(completed ? "TASK_SUBTASK_VERIFIED" : "TASK_SUBTASK_REOPENED", "TASK", taskId,
                subtask.getTask().getTitle(), null, null, null,
                (completed ? "Verified checklist item: " : "Reopened checklist item: ") + subtask.getTitle());
        if (completed) {
            subtaskRepository.flush();
            automationEngine.onSubtaskCompleted(subtask.getTask());
        }
        return toSubtaskDTO(saved);
    }

    @Override
    @Transactional
    public void deleteSubtask(Long taskId, Long subtaskId) {
        subtaskRepository.delete(requireSubtask(taskId, subtaskId));
    }

    private Subtask requireSubtask(Long taskId, Long subtaskId) {
        return subtaskRepository.findById(subtaskId)
                .filter(s -> s.getTask().getId().equals(taskId))
                .orElseThrow(() -> new NotFoundException("Checklist item not found: " + subtaskId));
    }

    private SubtaskDTO toSubtaskDTO(Subtask s) {
        return SubtaskDTO.builder()
                .id(s.getId())
                .taskId(s.getTask().getId())
                .title(s.getTitle())
                .isCompleted(s.getIsCompleted())
                .completedBy(s.getCompletedBy())
                .completedAt(s.getCompletedAt())
                .position(s.getPosition())
                .build();
    }

    // ---------------------------------------------------------------- attachments

    @Override
    @Transactional(readOnly = true)
    public List<TaskAttachmentDTO> getAttachments(Long taskId) {
        return attachmentRepository.findByTaskIdOrderByUploadedAtDesc(taskId).stream()
                .map(this::toAttachmentDTO)
                .toList();
    }

    @Override
    @Transactional
    public TaskAttachmentDTO addLinkAttachment(Long taskId, String name, String url) {
        Task task = requireTask(taskId);
        String cleanUrl = requireHttpUrl(url, "Link URL");
        String cleanName = name != null && !name.isBlank() ? requireText(name, "Name", 255) : cleanUrl;

        TaskAttachment saved = attachmentRepository.save(TaskAttachment.builder()
                .task(task)
                .fileName(cleanName.length() > 255 ? cleanName.substring(0, 255) : cleanName)
                .fileType("link")
                .fileUrl(cleanUrl)
                .uploadedBy(actorName())
                .uploadedAt(Instant.now())
                .build());
        auditLogUseCase.recordLog("TASK_ATTACHMENT_ADDED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Linked deliverable: " + saved.getFileName());
        return toAttachmentDTO(saved);
    }

    @Override
    @Transactional
    public TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file) {
        Task task = requireTask(taskId);
        String originalName = sanitizeFileName(file.getOriginalFilename());
        AttachmentStoragePort.StoredFile stored = attachmentStorage.store(taskId, file, file.getSize());

        TaskAttachment attachment = attachmentRepository.save(TaskAttachment.builder()
                .task(task)
                .fileName(originalName)
                .fileType(classify(file.getContentType(), originalName))
                .fileSize(humanSize(stored.sizeBytes()))
                .sizeBytes(stored.sizeBytes())
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .storageKey(stored.key())
                .fileUrl("pending")
                .uploadedBy(actorName())
                .uploadedAt(Instant.now())
                .build());
        attachment.setFileUrl("/api/v1/tasks/" + taskId + "/attachments/" + attachment.getId() + "/download");
        TaskAttachment saved = attachmentRepository.save(attachment);

        auditLogUseCase.recordLog("TASK_ATTACHMENT_ADDED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Uploaded deliverable: " + originalName + " (" + saved.getFileSize() + ")");
        return toAttachmentDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(Long taskId, Long attachmentId) {
        TaskAttachment attachment = requireAttachment(taskId, attachmentId);
        if (attachment.getStorageKey() == null) {
            throw new NotFoundException("This attachment is a link and has no stored file");
        }
        return new AttachmentDownload(attachmentStorage.load(attachment.getStorageKey()), attachment.getFileName(),
                attachment.getContentType(), attachment.getSizeBytes());
    }

    @Override
    @Transactional
    public void deleteAttachment(Long taskId, Long attachmentId) {
        TaskAttachment attachment = requireAttachment(taskId, attachmentId);
        attachmentRepository.delete(attachment);
        attachmentStorage.deleteAfterCommit(attachment.getStorageKey());
        auditLogUseCase.recordLog("TASK_ATTACHMENT_REMOVED", "TASK", taskId, attachment.getTask().getTitle(),
                null, null, null, "Removed deliverable: " + attachment.getFileName());
    }

    private TaskAttachment requireAttachment(Long taskId, Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getTask().getId().equals(taskId))
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));
    }

    private TaskAttachmentDTO toAttachmentDTO(TaskAttachment a) {
        return TaskAttachmentDTO.builder()
                .id(a.getId())
                .taskId(a.getTask().getId())
                .fileName(a.getFileName())
                .fileType(a.getFileType())
                .fileSize(a.getFileSize())
                .fileUrl(a.getFileUrl())
                .uploadedBy(a.getUploadedBy())
                .uploadedAt(a.getUploadedAt())
                .timeAgo(timeAgo(a.getUploadedAt()))
                .build();
    }

    static String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "attachment";
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (base.isEmpty()) return "attachment";
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    private static String classify(String contentType, String fileName) {
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (ct.startsWith("image/")) return "image";
        if (ct.equals("application/pdf") || lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".log") || lower.endsWith(".txt")) return "log";
        if (lower.matches(".*\\.(java|ts|js|json|xml|yml|yaml|sql|py|cs|go|csv|html|scss|css|sh)$")) return "code";
        return "document";
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ---------------------------------------------------------------- archive

    @Override
    @Transactional
    public TaskDTO archiveTask(Long id) {
        Task task = requireTask(id);
        task.setIsArchived(true);
        task.setArchivedAt(Instant.now());
        task.setArchivedBy(actorName());
        auditLogUseCase.recordLog("TASK_ARCHIVED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Task archived from the active board");
        return saveAndBroadcast(task);
    }

    @Override
    @Transactional
    public TaskDTO restoreTask(Long id) {
        Task task = requireTask(id);
        task.setIsArchived(false);
        task.setArchivedAt(null);
        task.setArchivedBy(null);
        auditLogUseCase.recordLog("TASK_RESTORED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Task restored to the active board");
        return saveAndBroadcast(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getArchivedTasks(Long projectId) {
        return taskMapper.toDTOs(taskRepositoryPort.findByProjectIdAndIsArchivedTrue(projectId));
    }

    // ---------------------------------------------------------------- dependencies

    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyDTO> getDependencies(Long taskId) {
        List<TaskDependency> outgoing = taskDependencyRepository.findBySourceTaskId(taskId);
        List<TaskDependency> incoming = taskDependencyRepository.findByTargetTaskId(taskId);

        Set<Long> relatedIds = new HashSet<>();
        outgoing.forEach(d -> relatedIds.add(d.getTargetTaskId()));
        incoming.forEach(d -> relatedIds.add(d.getSourceTaskId()));
        Map<Long, Task> related = taskRepositoryPort.findAllById(relatedIds).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        List<TaskDependencyDTO> list = new ArrayList<>();
        for (TaskDependency dep : outgoing) {
            list.add(toDependencyDTO(dep, taskId, related.get(dep.getTargetTaskId()), dep.getTargetTaskId(),
                    dep.getDependencyType(), dep.getTargetTaskTitle()));
        }
        for (TaskDependency dep : incoming) {
            list.add(toDependencyDTO(dep, taskId, related.get(dep.getSourceTaskId()), dep.getSourceTaskId(),
                    dep.getDependencyType().inverse(), "Task #" + dep.getSourceTaskId()));
        }
        return list;
    }

    @Override
    @Transactional
    public TaskDependencyDTO addDependency(Long sourceTaskId, Long targetTaskId, String type) {
        if (sourceTaskId.equals(targetTaskId)) {
            throw new DomainException("A task cannot depend on itself");
        }
        DependencyType dependencyType = DependencyType.parse(type);

        Task source = requireTask(sourceTaskId);
        Task target = requireTask(targetTaskId);
        if (!source.getProject().getId().equals(target.getProject().getId())) {
            throw new DomainException("Dependencies can only link tasks in the same project");
        }
        if (taskDependencyRepository.existsBySourceTaskIdAndTargetTaskId(sourceTaskId, targetTaskId)) {
            throw new ConflictException("These tasks are already linked");
        }

        TaskDependency saved = taskDependencyRepository.save(TaskDependency.builder()
                .sourceTaskId(sourceTaskId)
                .targetTaskId(targetTaskId)
                .targetTaskTitle(target.getTitle())
                .dependencyType(dependencyType)
                .createdBy(actorName())
                .createdAt(Instant.now())
                .build());
        auditLogUseCase.recordLog("TASK_DEPENDENCY_LINKED", "TASK", sourceTaskId, source.getTitle(), null, null, null,
                String.format("Linked %s -> task #%d: %s", dependencyType, targetTaskId, target.getTitle()));
        return toDependencyDTO(saved, sourceTaskId, target, targetTaskId, dependencyType, target.getTitle());
    }

    @Override
    @Transactional
    public void deleteDependency(Long dependencyId) {
        TaskDependency dep = taskDependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new NotFoundException("Dependency not found: " + dependencyId));
        taskDependencyRepository.delete(dep);
    }

    private TaskDependencyDTO toDependencyDTO(TaskDependency dep, Long viewingTaskId, Task other, Long otherId,
                                              DependencyType type, String fallbackTitle) {
        return TaskDependencyDTO.builder()
                .id(dep.getId())
                .sourceTaskId(viewingTaskId)
                .targetTaskId(otherId)
                .targetTaskTitle(other != null ? other.getTitle() : fallbackTitle)
                .targetTaskPriority(other != null ? other.getPriority().name() : TaskPriority.MEDIUM.name())
                .targetTaskProgress(other != null && other.getProgressPercentage() != null ? other.getProgressPercentage() : 0)
                .targetTaskStageName(other != null && other.getStage() != null ? other.getStage().getName() : "")
                .targetTaskAssignee(other != null && other.getAssignee() != null ? other.getAssignee().getFullName() : "Unassigned")
                .dependencyType(type.name())
                .isBlocked(other != null && Boolean.TRUE.equals(other.getIsBlocked()))
                .createdBy(dep.getCreatedBy())
                .createdAt(dep.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------- planning

    @Override
    @Transactional
    public TaskDTO updateStoryPoints(Long taskId, Integer points) {
        if (points == null || points < 0 || points > 100) {
            throw new DomainException("Story points must be between 0 and 100");
        }
        Task task = requireTask(taskId);
        task.setStoryPoints(points);
        return saveAndBroadcast(task);
    }

    @Override
    @Transactional
    public TaskDTO assignSprint(Long taskId, Long sprintId) {
        Task task = requireTask(taskId);
        if (sprintId != null) {
            Sprint sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new NotFoundException("Sprint not found: " + sprintId));
            if (!sprint.getProjectId().equals(task.getProject().getId())) {
                throw new DomainException("The sprint belongs to a different project");
            }
            if (sprint.getStatus() == SprintStatus.COMPLETED) {
                throw new ConflictException("Tasks cannot be added to a completed sprint");
            }
        }
        task.setSprintId(sprintId);
        return saveAndBroadcast(task);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowGateValidationResultDTO validateTransitionGate(Long taskId, Long targetStageId) {
        Task task = requireTask(taskId);
        return workflowGateService.evaluate(task, requireStageInProject(targetStageId, task.getProject().getId()));
    }

    @Override
    @Transactional
    public TaskDTO linkGit(Long taskId, GitLinkRequest request) {
        Task task = requireTask(taskId);
        task.setGitBranch(optionalText(request.getGitBranch(), "Branch", 150));
        task.setGitCommitHash(optionalText(request.getGitCommitHash(), "Commit hash", 64));
        String prUrl = request.getGitPrUrl() != null && !request.getGitPrUrl().isBlank()
                ? requireHttpUrl(request.getGitPrUrl(), "Pull request URL")
                : null;
        if (prUrl != null && prUrl.length() > 500) {
            throw new DomainException("Pull request URL is too long");
        }
        task.setGitPrUrl(prUrl);
        return saveAndBroadcast(task);
    }

    // ---------------------------------------------------------------- time tracking

    @Override
    @Transactional
    public TaskTimeLogDTO logTime(Long taskId, LogTimeRequest request) {
        Task task = requireTask(taskId);
        User user = actorEntity();
        if (user == null) {
            throw new ForbiddenException("An authenticated user is required to log time");
        }
        if (request.getHoursSpent() > 24) {
            throw new DomainException("Cannot log more than 24 hours in a single entry");
        }
        LocalDate loggedDate = request.getLoggedDate() != null ? request.getLoggedDate() : LocalDate.now();
        if (loggedDate.isAfter(LocalDate.now())) {
            throw new DomainException("Time cannot be logged for a future date");
        }

        TaskTimeLog saved = taskTimeLogRepository.save(TaskTimeLog.builder()
                .task(task)
                .user(user)
                .hoursSpent(request.getHoursSpent())
                .description(optionalText(request.getDescription(), "Description", 500))
                .loggedDate(loggedDate)
                .createdAt(Instant.now())
                .build());

        task.setTotalHoursSpent(taskTimeLogRepository.sumHoursByTaskId(taskId));
        if (request.getEstimatedHours() != null && request.getEstimatedHours() > 0) {
            task.setEstimatedHours(request.getEstimatedHours());
        }
        taskRepositoryPort.save(task);

        auditLogUseCase.recordLog("TIME_LOGGED", "TASK", taskId, task.getTitle(), null, null, null,
                "Logged " + request.getHoursSpent() + "h" + (request.getDescription() != null ? ": " + request.getDescription() : ""));
        return toTimeLogDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskTimeLogDTO> getTaskTimeLogs(Long taskId) {
        return taskTimeLogRepository.findByTaskIdOrderByCreatedAtDesc(taskId).stream()
                .map(this::toTimeLogDTO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTimeLog(Long logId) {
        TaskTimeLog entry = taskTimeLogRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Time log not found: " + logId));
        UserDetailsImpl user = currentUser.require();
        if (!entry.getUser().getId().equals(user.getId()) && !CurrentUser.isAdmin(user)) {
            throw new ForbiddenException("You can only delete your own time entries");
        }

        Task task = entry.getTask();
        taskTimeLogRepository.delete(entry);
        taskTimeLogRepository.flush();
        task.setTotalHoursSpent(taskTimeLogRepository.sumHoursByTaskId(task.getId()));
        taskRepositoryPort.save(task);
        auditLogUseCase.recordLog("TIME_LOG_DELETED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Removed " + entry.getHoursSpent() + "h entry from " + entry.getLoggedDate());
    }

    private TaskTimeLogDTO toTimeLogDTO(TaskTimeLog logEntry) {
        User u = logEntry.getUser();
        return TaskTimeLogDTO.builder()
                .id(logEntry.getId())
                .taskId(logEntry.getTask() != null ? logEntry.getTask().getId() : null)
                .taskTitle(logEntry.getTask() != null ? logEntry.getTask().getTitle() : null)
                .userId(u != null ? u.getId() : null)
                .userName(u != null ? u.getFullName() : "Unknown")
                .userEmail(u != null ? u.getEmail() : "")
                .userRole(u != null && u.getRole() != null ? u.getRole().getName() : "CONTRIBUTOR")
                .hoursSpent(logEntry.getHoursSpent())
                .description(logEntry.getDescription())
                .loggedDate(logEntry.getLoggedDate())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------- helpers

    private TaskDTO saveAndBroadcast(Task task) {
        TaskDTO dto = taskMapper.toDTO(taskRepositoryPort.save(task));
        taskNotificationPort.broadcastBoardUpdate(dto);
        return dto;
    }

    private Task requireTask(Long id) {
        return taskRepositoryPort.findById(id)
                .orElseThrow(() -> new NotFoundException("Task not found: " + id));
    }

    private Stage requireStageInProject(Long stageId, Long projectId) {
        Stage stage = stageRepositoryPort.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Stage not found: " + stageId));
        if (!stage.getProject().getId().equals(projectId)) {
            throw new DomainException("The stage belongs to a different project");
        }
        return stage;
    }

    private User requireActiveUser(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException(user.getFullName() + " is not an active user");
        }
        return user;
    }

    /** Managed entity of the current user, or null for system-initiated calls. */
    private User actorEntity() {
        return currentUser.get().flatMap(u -> userRepositoryPort.findById(u.getId())).orElse(null);
    }

    private String actorName() {
        return currentUser.get().map(UserDetailsImpl::getFullName).orElse("SyncFlow System");
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new DomainException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) return null;
        return requireText(value, field, maxLength);
    }

    /** Only absolute http(s) URLs are accepted, which rules out javascript:/data: links rendered in the UI. */
    static String requireHttpUrl(String value, String field) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if ((scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) {
                return uri.toString();
            }
        } catch (Exception ignored) {
            // fall through to the validation error
        }
        throw new DomainException(field + " must be an absolute http(s) URL");
    }

    private static String timeAgo(Instant at) {
        if (at == null) return "Just now";
        Duration d = Duration.between(at, Instant.now());
        if (d.toDays() > 0) return d.toDays() + "d ago";
        if (d.toHours() > 0) return d.toHours() + "h ago";
        if (d.toMinutes() > 0) return d.toMinutes() + "m ago";
        return "Just now";
    }
}
