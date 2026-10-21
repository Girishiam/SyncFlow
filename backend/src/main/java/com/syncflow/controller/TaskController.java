package com.syncflow.controller;

import com.syncflow.dto.CreateTaskRequest;
import com.syncflow.dto.MoveTaskRequest;
import com.syncflow.dto.SetBlockerRequest;
import com.syncflow.dto.TaskDTO;
import com.syncflow.dto.UpdateProgressRequest;
import com.syncflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TaskController {

    private final TaskService taskService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskDTO task = taskService.createTask(request);
        messagingTemplate.convertAndSend("/topic/board", task);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<TaskDTO> moveTask(@PathVariable Long id, @Valid @RequestBody MoveTaskRequest request) {
        TaskDTO task = taskService.moveTask(id, request);
        messagingTemplate.convertAndSend("/topic/board", task);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<TaskDTO> updateProgress(@PathVariable Long id, @Valid @RequestBody UpdateProgressRequest request) {
        TaskDTO task = taskService.updateProgress(id, request.getProgress());
        messagingTemplate.convertAndSend("/topic/board", task);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}/blocker")
    public ResponseEntity<TaskDTO> setBlocker(@PathVariable Long id, @Valid @RequestBody SetBlockerRequest request) {
        TaskDTO task = taskService.setBlocker(id, request);
        messagingTemplate.convertAndSend("/topic/board", task);
        if (Boolean.TRUE.equals(task.getIsBlocked())) {
            messagingTemplate.convertAndSend("/topic/blocker-alerts", task);
        }
        return ResponseEntity.ok(task);
    }
}
