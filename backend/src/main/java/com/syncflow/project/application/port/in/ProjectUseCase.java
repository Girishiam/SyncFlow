package com.syncflow.project.application.port.in;

import com.syncflow.dto.*;

import java.util.List;

public interface ProjectUseCase {
    List<ProjectDTO> getAllProjects();
    ProjectBoardDTO getProjectBoard(Long projectId);
    StageDTO createStage(Long projectId, CreateStageRequest request);
    void reorderStages(Long projectId, ReorderStagesRequest request);
}
