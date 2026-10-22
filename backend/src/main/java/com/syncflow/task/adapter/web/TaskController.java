package com.syncflow.task.adapter.web;

import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.dto.*;
import com.syncflow.model.User;
import com.syncflow.security.UserDetailsImpl;
import com.syncflow.task.application.port.in.TaskUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskController {

    private final TaskUseCase taskUseCase;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskDetails(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getTaskDetails(id));
    }

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.createTask(request));
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<TaskDTO> moveTask(@PathVariable Long id, @Valid @RequestBody MoveTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.moveTask(id, request));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<TaskDTO> updateProgress(@PathVariable Long id, @Valid @RequestBody UpdateProgressRequest request) {
        return ResponseEntity.ok(taskUseCase.updateProgress(id, request.getProgress()));
    }

    @PutMapping("/{id}/blocker")
    public ResponseEntity<TaskDTO> setBlocker(@PathVariable Long id, @Valid @RequestBody SetBlockerRequest request) {
        return ResponseEntity.ok(taskUseCase.setBlocker(id, request));
    }

    @PutMapping("/{id}/assignee")
    public ResponseEntity<TaskDTO> assignTask(@PathVariable Long id, @Valid @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskUseCase.assignTask(id, request));
    }

    @PutMapping("/{id}/deadline")
    public ResponseEntity<TaskDTO> updateDeadline(@PathVariable Long id, @Valid @RequestBody UpdateDeadlineRequest request) {
        return ResponseEntity.ok(taskUseCase.updateDeadline(id, request));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<TaskCommentDTO>> getTaskComments(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getTaskComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskCommentDTO> addComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody AddCommentRequest request) {

        UUID authorId = userDetails != null ? userDetails.getId() : null;
        if (authorId == null) {
            authorId = userRepositoryPort.findAll().stream().findFirst().map(User::getId).orElse(null);
        }

        if (authorId == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(taskUseCase.addComment(id, request, authorId));
    }

    // --- Subtasks / QA Checklist Endpoints ---
    @GetMapping("/{id}/subtasks")
    public ResponseEntity<List<com.syncflow.dto.SubtaskDTO>> getSubtasks(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getSubtasks(id));
    }

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<com.syncflow.dto.SubtaskDTO> addSubtask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody java.util.Map<String, String> body) {
        String title = body.getOrDefault("title", "");
        String author = userDetails != null ? userDetails.getUsername() : "Team Member";
        return ResponseEntity.ok(taskUseCase.addSubtask(id, title, author));
    }

    @PatchMapping("/{id}/subtasks/{subtaskId}/toggle")
    public ResponseEntity<com.syncflow.dto.SubtaskDTO> toggleSubtask(
            @PathVariable Long id,
            @PathVariable Long subtaskId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String actor = userDetails != null ? userDetails.getUsername() : "Team Member";
        return ResponseEntity.ok(taskUseCase.toggleSubtask(id, subtaskId, actor));
    }

    @DeleteMapping("/{id}/subtasks/{subtaskId}")
    public ResponseEntity<Void> deleteSubtask(
            @PathVariable Long id,
            @PathVariable Long subtaskId) {
        taskUseCase.deleteSubtask(id, subtaskId);
        return ResponseEntity.ok().build();
    }

    // --- Task Attachments & Screenshots Endpoints ---
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<com.syncflow.dto.TaskAttachmentDTO>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getAttachments(id));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<com.syncflow.dto.TaskAttachmentDTO> addAttachment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody java.util.Map<String, String> body) {
        String fileName = body.getOrDefault("fileName", "attachment.pdf");
        String fileType = body.getOrDefault("fileType", "document");
        String fileSize = body.getOrDefault("fileSize", "1.4 MB");
        String fileUrl = body.getOrDefault("fileUrl", "#");
        String uploadedBy = userDetails != null ? userDetails.getUsername() : "Team Member";

        return ResponseEntity.ok(taskUseCase.addAttachment(id, fileName, fileType, fileSize, fileUrl, uploadedBy));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId) {
        taskUseCase.deleteAttachment(id, attachmentId);
        return ResponseEntity.ok().build();
    }

    // --- Task Archiving & Board Lifecycle Endpoints ---
    @PutMapping("/{id}/archive")
    public ResponseEntity<com.syncflow.dto.TaskDTO> archiveTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String operator = userDetails != null ? userDetails.getUsername() : "Operator";
        return ResponseEntity.ok(taskUseCase.archiveTask(id, operator));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<com.syncflow.dto.TaskDTO> restoreTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String operator = userDetails != null ? userDetails.getUsername() : "Operator";
        return ResponseEntity.ok(taskUseCase.restoreTask(id, operator));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<com.syncflow.dto.TaskDTO>> getArchivedTasks(@RequestParam Long projectId) {
        return ResponseEntity.ok(taskUseCase.getArchivedTasks(projectId));
    }

    // --- Task Dependencies Endpoints ---
    @GetMapping("/{id}/dependencies")
    public ResponseEntity<List<com.syncflow.dto.TaskDependencyDTO>> getDependencies(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getDependencies(id));
    }

    @PostMapping("/{id}/dependencies")
    public ResponseEntity<com.syncflow.dto.TaskDependencyDTO> addDependency(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody java.util.Map<String, String> body) {
        Long targetTaskId = Long.parseLong(body.get("targetTaskId"));
        String type = body.getOrDefault("dependencyType", "BLOCKS");
        String operator = userDetails != null ? userDetails.getUsername() : "Operator";
        return ResponseEntity.ok(taskUseCase.addDependency(id, targetTaskId, type, operator));
    }

    @DeleteMapping("/dependencies/{dependencyId}")
    public ResponseEntity<Void> deleteDependency(@PathVariable Long dependencyId) {
        taskUseCase.deleteDependency(dependencyId);
        return ResponseEntity.ok().build();
    }

    // --- Story Points & Sprint Assignment Endpoints ---
    @PutMapping("/{id}/story-points")
    public ResponseEntity<com.syncflow.dto.TaskDTO> updateStoryPoints(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Integer> body) {
        Integer points = body.getOrDefault("storyPoints", 3);
        return ResponseEntity.ok(taskUseCase.updateStoryPoints(id, points));
    }

    @PutMapping("/{id}/sprint")
    public ResponseEntity<com.syncflow.dto.TaskDTO> assignSprint(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body) {
        Long sprintId = body.get("sprintId");
        return ResponseEntity.ok(taskUseCase.assignSprint(id, sprintId));
    }

    // --- Enterprise Transition Policy & Quality Gate Check ---
    @GetMapping("/{id}/validate-gate")
    public ResponseEntity<com.syncflow.dto.WorkflowGateValidationResultDTO> validateTransitionGate(
            @PathVariable Long id,
            @RequestParam Long targetStageId) {
        return ResponseEntity.ok(taskUseCase.validateTransitionGate(id, targetStageId));
    }

    // --- Git Repository & Pull Request Integration ---
    @PutMapping("/{id}/git")
    public ResponseEntity<com.syncflow.dto.TaskDTO> linkGit(
            @PathVariable Long id,
            @RequestBody com.syncflow.dto.GitLinkRequest request) {
        return ResponseEntity.ok(taskUseCase.linkGit(id, request));
    }

    // --- Task Work Logs & Time Tracking ---
    @PostMapping("/{id}/time-logs")
    public ResponseEntity<com.syncflow.dto.TaskTimeLogDTO> logTime(
            @PathVariable Long id,
            @Valid @RequestBody com.syncflow.dto.LogTimeRequest request,
            org.springframework.security.core.Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(taskUseCase.logTime(id, request, userEmail));
    }

    @GetMapping("/{id}/time-logs")
    public ResponseEntity<List<com.syncflow.dto.TaskTimeLogDTO>> getTaskTimeLogs(@PathVariable Long id) {
        return ResponseEntity.ok(taskUseCase.getTaskTimeLogs(id));
    }

    @DeleteMapping("/time-logs/{logId}")
    public ResponseEntity<Void> deleteTimeLog(
            @PathVariable Long logId,
            org.springframework.security.core.Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        taskUseCase.deleteTimeLog(logId, userEmail);
        return ResponseEntity.ok().build();
    }
}
