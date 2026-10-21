package com.syncflow.service;

import com.syncflow.dto.ProjectBoardDTO;
import com.syncflow.dto.StageDTO;
import com.syncflow.dto.TaskDTO;
import com.syncflow.model.Project;
import com.syncflow.model.Stage;
import com.syncflow.model.Task;
import com.syncflow.repository.ProjectRepository;
import com.syncflow.repository.StageRepository;
import com.syncflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<com.syncflow.dto.ProjectDTO> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> com.syncflow.dto.ProjectDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .status(p.getStatus())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectBoardDTO getProjectBoard(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectId));

        List<Stage> stages = stageRepository.findByProjectIdOrderByPositionAsc(projectId);
        List<Task> allTasks = taskRepository.findByProjectId(projectId);

        List<StageDTO> stageDTOs = new ArrayList<>();
        long totalTasks = allTasks.size();
        long blockedTasks = 0;
        long completedTasks = 0;

        for (Stage stage : stages) {
            List<TaskDTO> stageTasks = new ArrayList<>();
            for (Task t : allTasks) {
                if (t.getStage() != null && t.getStage().getId().equals(stage.getId())) {
                    if (Boolean.TRUE.equals(t.getIsBlocked())) {
                        blockedTasks++;
                    }
                    if (Boolean.TRUE.equals(stage.getIsDoneStage()) || (t.getProgressPercentage() != null && t.getProgressPercentage() == 100)) {
                        completedTasks++;
                    }

                    String assigneeName = t.getAssignee() != null ? t.getAssignee().getFullName() : "Unassigned";
                    String avatar = getInitials(assigneeName);

                    stageTasks.add(TaskDTO.builder()
                            .id(t.getId())
                            .title(t.getTitle())
                            .description(t.getDescription())
                            .priority(t.getPriority())
                            .progress(t.getProgressPercentage() != null ? t.getProgressPercentage() : 0)
                            .stageId(stage.getId())
                            .stageName(stage.getName())
                            .projectId(project.getId())
                            .projectName(project.getName())
                            .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                            .assignee(assigneeName)
                            .avatar(avatar)
                            .isBlocked(Boolean.TRUE.equals(t.getIsBlocked()))
                            .blockerReason(t.getBlockerReason())
                            .build());
                }
            }

            stageDTOs.add(StageDTO.builder()
                    .id(stage.getId())
                    .name(stage.getName())
                    .position(stage.getPosition())
                    .isDoneStage(stage.getIsDoneStage())
                    .tasks(stageTasks)
                    .build());
        }

        int completionPercentage = totalTasks > 0 ? (int) Math.round(((double) completedTasks / totalTasks) * 100) : 0;

        return ProjectBoardDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .stages(stageDTOs)
                .totalTasks(totalTasks)
                .blockedTasks(blockedTasks)
                .completionPercentage(completionPercentage)
                .build();
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "UN";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }
}
