package com.syncflow.controller;

import com.syncflow.dto.ProjectBoardDTO;
import com.syncflow.model.Project;
import com.syncflow.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<com.syncflow.dto.ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}/board")
    public ResponseEntity<ProjectBoardDTO> getProjectBoard(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectBoard(id));
    }
}
