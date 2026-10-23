package com.syncflow.task.adapter.web;

import com.syncflow.dto.*;
import com.syncflow.task.application.port.in.TaskUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Task endpoints. Every route checks the role permission ({@code @access.can}) and that the caller
 * may access the owning project ({@code @access.task} / {@code @access.project}).
 */
@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final String VIEW = "@access.can('TASK:VIEW') and @access.task(#id)";
    private static final String EDIT = "@access.can('TASK:EDIT') and @access.task(#id)";

    private final TaskUseCase taskUseCase;

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public ResponseEntity<TaskDTO> getTaskDetails(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getTaskDetails(id));
    }

    @PostMapping
    @PreAuthorize("@access.can('TASK:CREATE') and @access.project(#request.projectId)")
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.createTask(request));
    }

    @PutMapping("/{id}/move")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> moveTask(@PathVariable Long id, @Valid @RequestBody MoveTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.moveTask(id, request));
    }

    @PutMapping("/{id}/progress")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> updateProgress(@PathVariable Long id, @Valid @RequestBody UpdateProgressRequest request) {
        return ResponseEntity.ok(taskUseCase.updateProgress(id, request.getProgress()));
    }

    @PutMapping("/{id}/blocker")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> setBlocker(@PathVariable Long id, @Valid @RequestBody SetBlockerRequest request) {
        return ResponseEntity.ok(taskUseCase.setBlocker(id, request));
    }

    @PutMapping("/{id}/assignee")
    @PreAuthorize("@access.can('TASK:ASSIGN') and @access.task(#id)")
    public ResponseEntity<TaskDTO> assignTask(@PathVariable Long id, @Valid @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.assignTask(id, request));
    }

    @PutMapping("/{id}/deadline")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> updateDeadline(@PathVariable Long id, @Valid @RequestBody UpdateDeadlineRequest request) {
        return ResponseEntity.ok(taskUseCase.updateDeadline(id, request));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<TaskCommentDTO>> getTaskComments(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getTaskComments(id));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize(VIEW)
    public ResponseEntity<TaskCommentDTO> addComment(@PathVariable Long id, @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.ok(taskUseCase.addComment(id, request));
    }

    // --- Checklist ---
    @GetMapping("/{id}/subtasks")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<SubtaskDTO>> getSubtasks(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getSubtasks(id));
    }

    @PostMapping("/{id}/subtasks")
    @PreAuthorize(EDIT)
    public ResponseEntity<SubtaskDTO> addSubtask(@PathVariable Long id, @Valid @RequestBody TitleRequest body) {
        return ResponseEntity.ok(taskUseCase.addSubtask(id, body.getTitle()));
    }

    @PatchMapping("/{id}/subtasks/{subtaskId}/toggle")
    @PreAuthorize(EDIT)
    public ResponseEntity<SubtaskDTO> toggleSubtask(@PathVariable Long id, @PathVariable Long subtaskId) {
        return ResponseEntity.ok(taskUseCase.toggleSubtask(id, subtaskId));
    }

    @DeleteMapping("/{id}/subtasks/{subtaskId}")
    @PreAuthorize(EDIT)
    public ResponseEntity<Void> deleteSubtask(@PathVariable Long id, @PathVariable Long subtaskId) {
        taskUseCase.deleteSubtask(id, subtaskId);
        return ResponseEntity.noContent().build();
    }

    // --- Attachments ---
    @GetMapping("/{id}/attachments")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<TaskAttachmentDTO>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getAttachments(id));
    }

    /** Uploads a file (multipart field {@code file}). */
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskAttachmentDTO> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(taskUseCase.uploadAttachment(id, file));
    }

    /** Adds an external http(s) link as a deliverable. */
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskAttachmentDTO> addLinkAttachment(@PathVariable Long id, @Valid @RequestBody LinkAttachmentRequest body) {
        return ResponseEntity.ok(taskUseCase.addLinkAttachment(id, body.getFileName(), body.getFileUrl()));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    @PreAuthorize(VIEW)
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        TaskUseCase.AttachmentDownload download = taskUseCase.downloadAttachment(id, attachmentId);
        MediaType type;
        try {
            type = download.contentType() != null ? MediaType.parseMediaType(download.contentType()) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox");
        if (download.sizeBytes() != null) {
            response.contentLength(download.sizeBytes());
        }
        return response.body(download.resource());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize(EDIT)
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        taskUseCase.deleteAttachment(id, attachmentId);
        return ResponseEntity.noContent().build();
    }

    // --- Archiving ---
    @PutMapping("/{id}/archive")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> archiveTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.archiveTask(id));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> restoreTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.restoreTask(id));
    }

    @GetMapping("/archived")
    @PreAuthorize("@access.can('TASK:VIEW') and @access.project(#projectId)")
    public ResponseEntity<List<TaskDTO>> getArchivedTasks(@RequestParam Long projectId) {
        return ResponseEntity.ok(taskUseCase.getArchivedTasks(projectId));
    }

    // --- Dependencies ---
    @GetMapping("/{id}/dependencies")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<TaskDependencyDTO>> getDependencies(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getDependencies(id));
    }

    @PostMapping("/{id}/dependencies")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDependencyDTO> addDependency(@PathVariable Long id, @Valid @RequestBody DependencyRequest body) {
        return ResponseEntity.ok(taskUseCase.addDependency(id, body.getTargetTaskId(), body.getDependencyType()));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    @PreAuthorize("@access.can('TASK:EDIT') and @access.dependency(#dependencyId)")
    public ResponseEntity<Void> deleteDependency(@PathVariable Long dependencyId) {
        taskUseCase.deleteDependency(dependencyId);
        return ResponseEntity.noContent().build();
    }

    // --- Story Points & Sprint ---
    @PutMapping("/{id}/story-points")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> updateStoryPoints(@PathVariable Long id, @Valid @RequestBody StoryPointsRequest body) {
        return ResponseEntity.ok(taskUseCase.updateStoryPoints(id, body.getStoryPoints()));
    }

    @PutMapping("/{id}/sprint")
    @PreAuthorize("@access.can('SPRINT:MANAGE') and @access.task(#id)")
    public ResponseEntity<TaskDTO> assignSprint(@PathVariable Long id, @RequestBody SprintAssignmentRequest body) {
        return ResponseEntity.ok(taskUseCase.assignSprint(id, body.getSprintId()));
    }

    // --- Workflow gates ---
    @GetMapping("/{id}/validate-gate")
    @PreAuthorize(VIEW)
    public ResponseEntity<WorkflowGateValidationResultDTO> validateTransitionGate(@PathVariable Long id, @RequestParam Long targetStageId) {
        return ResponseEntity.ok(taskUseCase.validateTransitionGate(id, targetStageId));
    }

    // --- Git ---
    @PutMapping("/{id}/git")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskDTO> linkGit(@PathVariable Long id, @RequestBody GitLinkRequest request) {
        return ResponseEntity.ok(taskUseCase.linkGit(id, request));
    }

    // --- Time tracking ---
    @PostMapping("/{id}/time-logs")
    @PreAuthorize(EDIT)
    public ResponseEntity<TaskTimeLogDTO> logTime(@PathVariable Long id, @Valid @RequestBody LogTimeRequest request) {
        return ResponseEntity.ok(taskUseCase.logTime(id, request));
    }

    @GetMapping("/{id}/time-logs")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<TaskTimeLogDTO>> getTaskTimeLogs(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getTaskTimeLogs(id));
    }

    @DeleteMapping("/time-logs/{logId}")
    @PreAuthorize("@access.timeLog(#logId)")
    public ResponseEntity<Void> deleteTimeLog(@PathVariable Long logId) {
        taskUseCase.deleteTimeLog(logId);
        return ResponseEntity.noContent().build();
    }

    // --- Request bodies ---
    @Data
    public static class TitleRequest {
        @NotBlank
        private String title;
    }

    @Data
    public static class LinkAttachmentRequest {
        private String fileName;
        @NotBlank
        private String fileUrl;
    }

    @Data
    public static class DependencyRequest {
        @NotNull
        private Long targetTaskId;
        private String dependencyType;
    }

    @Data
    public static class StoryPointsRequest {
        @NotNull
        private Integer storyPoints;
    }

    @Data
    public static class SprintAssignmentRequest {
        /** Null moves the task back to the backlog. */
        private Long sprintId;
    }
}
