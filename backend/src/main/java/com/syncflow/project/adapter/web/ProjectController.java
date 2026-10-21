package com.syncflow.project.adapter.web;

import com.syncflow.dto.*;
import com.syncflow.project.application.port.in.ProjectUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectUseCase.getAllProjects());
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectUseCase.createProject(request));
    }

    @GetMapping("/{id}/board")
    public ResponseEntity<ProjectBoardDTO> getProjectBoard(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectBoard(id));
    }

    @PostMapping("/{id}/stages")
    public ResponseEntity<StageDTO> createStage(
            @PathVariable Long id,
            @Valid @RequestBody CreateStageRequest request) {
        return ResponseEntity.ok(projectUseCase.createStage(id, request));
    }

    @PutMapping("/{id}/stages/reorder")
    public ResponseEntity<Void> reorderStages(
            @PathVariable Long id,
            @Valid @RequestBody ReorderStagesRequest request) {
        projectUseCase.reorderStages(id, request);
        return ResponseEntity.ok().build();
    }
}
