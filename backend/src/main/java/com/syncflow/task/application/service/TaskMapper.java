package com.syncflow.task.application.service;

import com.syncflow.project.application.port.out.SprintRepositoryPort;
import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.application.port.out.TaskAttachmentRepositoryPort;

import com.syncflow.model.enums.*;

import com.syncflow.dto.TaskDTO;
import com.syncflow.model.Sprint;
import com.syncflow.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Single source of truth for Task -> TaskDTO so that board loads, REST responses and live
 * WebSocket updates always carry the same fields.
 */
@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final SprintRepositoryPort sprintRepository;
    private final SubtaskRepositoryPort subtaskRepository;
    private final TaskAttachmentRepositoryPort attachmentRepository;

    public TaskDTO toDTO(Task task) {
        Map<Long, String> sprintNames = task.getSprintId() == null
                ? Map.of()
                : sprintRepository.findById(task.getSprintId())
                        .map(s -> Map.of(s.getId(), s.getName()))
                        .orElse(Map.of());
        List<Long> id = task.getId() != null ? List.of(task.getId()) : List.of();
        return toDTO(task, sprintNames, subtaskRepository.checklistCounts(id), attachmentRepository.attachmentCounts(id));
    }

    /** Maps many tasks with a single sprint lookup. */
    public List<TaskDTO> toDTOs(Collection<Task> tasks) {
        Set<Long> sprintIds = tasks.stream()
                .map(Task::getSprintId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> sprintNames = sprintIds.isEmpty()
                ? Map.of()
                : sprintRepository.findAllById(sprintIds).stream()
                        .collect(Collectors.toMap(Sprint::getId, Sprint::getName, (a, b) -> a));
        List<Long> ids = tasks.stream().map(Task::getId).filter(Objects::nonNull).toList();
        Map<Long, long[]> checklists = ids.isEmpty() ? Map.of() : subtaskRepository.checklistCounts(ids);
        Map<Long, Long> attachments = ids.isEmpty() ? Map.of() : attachmentRepository.attachmentCounts(ids);
        return tasks.stream().map(t -> toDTO(t, sprintNames, checklists, attachments)).toList();
    }

    private TaskDTO toDTO(Task task, Map<Long, String> sprintNames, Map<Long, long[]> checklists, Map<Long, Long> attachments) {
        long[] checklist = checklists.getOrDefault(task.getId(), new long[]{0, 0});
        String assigneeName = task.getAssignee() != null ? task.getAssignee().getFullName() : "Unassigned";
        return TaskDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority() != null ? task.getPriority().name() : null)
                .progress(task.getProgressPercentage() != null ? task.getProgressPercentage() : 0)
                .stageId(task.getStage() != null ? task.getStage().getId() : null)
                .stageName(task.getStage() != null ? task.getStage().getName() : "")
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : "")
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assignee(assigneeName)
                .avatar(initials(assigneeName))
                .isBlocked(Boolean.TRUE.equals(task.getIsBlocked()))
                .blockerReason(task.getBlockerReason())
                .deadline(task.getDeadline())
                .isArchived(Boolean.TRUE.equals(task.getIsArchived()))
                .archivedAt(task.getArchivedAt())
                .archivedBy(task.getArchivedBy())
                .startDate(task.getStartDate())
                .completedAt(task.getCompletedAt())
                .sprintId(task.getSprintId())
                .sprintName(task.getSprintId() != null ? sprintNames.get(task.getSprintId()) : null)
                .storyPoints(task.getStoryPoints() != null ? task.getStoryPoints() : 3)
                .gitBranch(task.getGitBranch())
                .gitCommitHash(task.getGitCommitHash())
                .gitPrUrl(task.getGitPrUrl())
                .estimatedHours(task.getEstimatedHours() != null ? task.getEstimatedHours() : 8.0)
                .totalHoursSpent(task.getTotalHoursSpent() != null ? task.getTotalHoursSpent() : 0.0)
                .checklistTotal((int) checklist[0])
                .checklistDone((int) checklist[1])
                .attachmentCount(attachments.getOrDefault(task.getId(), 0L).intValue())
                .build();
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "UN";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    /** A task is complete exactly when it sits in a done stage. */
    public static boolean isDone(Task task) {
        return task.getStage() != null && Boolean.TRUE.equals(task.getStage().getIsDoneStage());
    }
}
