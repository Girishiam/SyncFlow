package com.syncflow.project.application.port.in;

import com.syncflow.dto.ProjectBoardDTO;
import com.syncflow.dto.ProjectDTO;

import java.util.List;

public interface ProjectUseCase {
    List<ProjectDTO> getAllProjects();
    ProjectBoardDTO getProjectBoard(Long projectId);
}
