package com.syncflow.task.application.service;

import com.syncflow.task.application.port.out.TaskDependencyRepositoryPort;
import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.application.port.out.TaskAttachmentRepositoryPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;

import com.syncflow.model.enums.*;

import com.syncflow.dto.WorkflowGateValidationResultDTO;
import com.syncflow.model.Stage;
import com.syncflow.model.Subtask;
import com.syncflow.model.Task;
import com.syncflow.model.TaskDependency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates the quality gates configured on a target stage
 * ({@link Stage#getRequiresChecklistComplete()}, {@link Stage#getRequiresNoBlockers()},
 * {@link Stage#getRequiresDeliverable()}) for a task about to enter it.
 */
@Service
@RequiredArgsConstructor
public class WorkflowGateService {

    private final SubtaskRepositoryPort subtaskRepository;
    private final TaskAttachmentRepositoryPort attachmentRepository;
    private final TaskDependencyRepositoryPort taskDependencyRepository;
    private final TaskRepositoryPort taskRepository;

    public WorkflowGateValidationResultDTO evaluate(Task task, Stage targetStage) {
        List<String> violations = new ArrayList<>();
        List<String> blockingTaskTitles = new ArrayList<>();
        int uncompletedSubtasks = 0;
        boolean hasDeliverable = false;

        if (Boolean.TRUE.equals(targetStage.getRequiresChecklistComplete())) {
            for (Subtask st : subtaskRepository.findByTaskIdOrderByPositionAsc(task.getId())) {
                if (!Boolean.TRUE.equals(st.getIsCompleted())) {
                    uncompletedSubtasks++;
                }
            }
            if (uncompletedSubtasks > 0) {
                violations.add(uncompletedSubtasks + " checklist item(s) are still open; '" + targetStage.getName()
                        + "' requires a completed checklist.");
            }
        }

        if (Boolean.TRUE.equals(targetStage.getRequiresNoBlockers())) {
            if (Boolean.TRUE.equals(task.getIsBlocked())) {
                violations.add("Task is flagged as blocked: "
                        + (task.getBlockerReason() != null ? task.getBlockerReason() : "unspecified blocker"));
            }
            blockingTaskTitles.addAll(openBlockers(task.getId()));
            if (!blockingTaskTitles.isEmpty()) {
                violations.add("Unresolved blocking tasks: " + String.join(", ", blockingTaskTitles));
            }
        }

        if (Boolean.TRUE.equals(targetStage.getRequiresDeliverable())) {
            hasDeliverable = !attachmentRepository.findByTaskIdOrderByUploadedAtDesc(task.getId()).isEmpty()
                    || (task.getGitPrUrl() != null && !task.getGitPrUrl().isBlank());
            if (!hasDeliverable) {
                violations.add("At least one deliverable attachment or linked pull request is required.");
            }
        }

        boolean requiresOverride = !violations.isEmpty();
        return WorkflowGateValidationResultDTO.builder()
                .allowed(!requiresOverride)
                .targetStageName(targetStage.getName())
                .requiresOverride(requiresOverride)
                .violations(violations)
                .blockingTaskTitles(blockingTaskTitles)
                .uncompletedSubtasksCount(uncompletedSubtasks)
                .hasDeliverableAttachment(hasDeliverable)
                .build();
    }

    /**
     * Titles of unfinished tasks that block {@code taskId}: either this task declares
     * "BLOCKED_BY" another task, or another task declares that it "BLOCKS" this one.
     */
    private List<String> openBlockers(Long taskId) {
        Set<Long> blockerIds = new HashSet<>();
        for (TaskDependency dep : taskDependencyRepository.findBySourceTaskId(taskId)) {
            if (dep.getDependencyType() == DependencyType.BLOCKED_BY) {
                blockerIds.add(dep.getTargetTaskId());
            }
        }
        for (TaskDependency dep : taskDependencyRepository.findByTargetTaskId(taskId)) {
            if (dep.getDependencyType() == DependencyType.BLOCKS) {
                blockerIds.add(dep.getSourceTaskId());
            }
        }
        if (blockerIds.isEmpty()) {
            return List.of();
        }
        return taskRepository.findAllById(blockerIds).stream()
                .filter(t -> !TaskMapper.isDone(t))
                .map(Task::getTitle)
                .toList();
    }
}
