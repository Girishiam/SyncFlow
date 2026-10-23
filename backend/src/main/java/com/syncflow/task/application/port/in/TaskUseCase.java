package com.syncflow.task.application.port.in;

import com.syncflow.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Task workflow use cases. The acting user is always the authenticated principal.
 */
public interface TaskUseCase {
    TaskDTO createTask(CreateTaskRequest request);
    TaskDTO moveTask(Long id, MoveTaskRequest request);
    TaskDTO updateProgress(Long id, Integer progress);
    TaskDTO setBlocker(Long id, SetBlockerRequest request);
    TaskDTO getTaskDetails(Long id);
    List<TaskCommentDTO> getTaskComments(Long taskId);
    TaskCommentDTO addComment(Long taskId, AddCommentRequest request);
    TaskDTO assignTask(Long id, AssignTaskRequest request);
    TaskDTO updateDeadline(Long id, UpdateDeadlineRequest request);

    // Subtasks & Deliverable QA Checklist
    List<SubtaskDTO> getSubtasks(Long taskId);
    SubtaskDTO addSubtask(Long taskId, String title);
    SubtaskDTO toggleSubtask(Long taskId, Long subtaskId);
    void deleteSubtask(Long taskId, Long subtaskId);

    // Attachments: uploaded files and external links
    List<TaskAttachmentDTO> getAttachments(Long taskId);
    TaskAttachmentDTO addLinkAttachment(Long taskId, String name, String url);
    TaskAttachmentDTO uploadAttachment(Long taskId, MultipartFile file);
    AttachmentDownload downloadAttachment(Long taskId, Long attachmentId);
    void deleteAttachment(Long taskId, Long attachmentId);

    record AttachmentDownload(Resource resource, String fileName, String contentType, Long sizeBytes) {
    }

    // Archiving & Lifecycle
    TaskDTO archiveTask(Long id);
    TaskDTO restoreTask(Long id);
    List<TaskDTO> getArchivedTasks(Long projectId);

    // Dependencies & Blocker Links
    List<TaskDependencyDTO> getDependencies(Long taskId);
    TaskDependencyDTO addDependency(Long sourceTaskId, Long targetTaskId, String type);
    void deleteDependency(Long dependencyId);

    // Sprint & Story Points
    TaskDTO updateStoryPoints(Long taskId, Integer points);
    TaskDTO assignSprint(Long taskId, Long sprintId);

    // Quality Gates
    WorkflowGateValidationResultDTO validateTransitionGate(Long taskId, Long targetStageId);

    // Git / VCS Connectors
    TaskDTO linkGit(Long taskId, GitLinkRequest request);

    // Time Tracking
    TaskTimeLogDTO logTime(Long taskId, LogTimeRequest request);
    List<TaskTimeLogDTO> getTaskTimeLogs(Long taskId);
    void deleteTimeLog(Long logId);
}
