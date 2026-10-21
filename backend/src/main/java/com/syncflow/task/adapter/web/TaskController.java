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
}
