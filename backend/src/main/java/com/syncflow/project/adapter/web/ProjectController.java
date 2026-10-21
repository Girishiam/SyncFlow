package com.syncflow.project.adapter.web;

import com.syncflow.dto.ProjectBoardDTO;
import com.syncflow.dto.ProjectDTO;
import com.syncflow.project.application.port.in.ProjectUseCase;
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

    @GetMapping("/{id}/board")
    public ResponseEntity<ProjectBoardDTO> getProjectBoard(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectBoard(id));
    }
}
