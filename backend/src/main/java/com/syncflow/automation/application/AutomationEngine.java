package com.syncflow.automation.application;

import com.syncflow.project.application.port.out.AutomationRuleRepositoryPort;
import com.syncflow.task.application.port.out.SubtaskRepositoryPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.auth.application.port.out.UserRepositoryPort;

import com.syncflow.model.enums.*;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.common.event.ProjectEvent;
import com.syncflow.dto.TaskDTO;
import com.syncflow.dto.WorkflowGateValidationResultDTO;
import com.syncflow.model.*;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import com.syncflow.task.application.service.TaskMapper;
import com.syncflow.task.application.service.WorkflowGateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Executes the per-project automation rules ("Butler") in response to task and sprint events.
 *
 * <p>Supported trigger / action pairs:</p>
 * <ul>
 *   <li>{@code MOVED_TO_REVIEW} / {@code ASSIGN_ROLE}: assign the least-loaded available member of {@code targetRole}</li>
 *   <li>{@code TASK_BLOCKED} / {@code NOTIFY_MANAGER}: escalate blocked CRITICAL tasks to {@code targetRole}</li>
 *   <li>{@code ALL_SUBTASKS_COMPLETE} / {@code MOVE_TO_DONE}: move the task to the done stage if its gates pass</li>
 *   <li>{@code SPRINT_CLOSED} / {@code ARCHIVE_DONE_TASKS}: archive finished tasks of the closed sprint</li>
 * </ul>
 * Rules run inside the caller's transaction, so their effects commit or roll back with it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationEngine {

    private final AutomationRuleRepositoryPort automationRuleRepository;
    private final StageRepositoryPort stageRepositoryPort;
    private final TaskRepositoryPort taskRepository;
    private final UserRepositoryPort userRepository;
    private final ProjectRepositoryPort projectRepository;
    private final SubtaskRepositoryPort subtaskRepository;
    private final WorkflowGateService workflowGateService;
    private final NotificationUseCase notificationUseCase;
    private final TaskNotificationPort taskNotificationPort;
    private final TaskMapper taskMapper;
    private final AuditLogUseCase auditLogUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public void onTaskMoved(Task task, Stage targetStage) {
        if (!isReviewStage(targetStage)) return;
        for (AutomationRule rule : rules(task, AutomationTrigger.MOVED_TO_REVIEW)) {
            if (rule.getActionType() == AutomationAction.ASSIGN_ROLE && rule.getTargetRole() != null) {
                assignLeastLoaded(task, normalizeRole(rule.getTargetRole())).ifPresent(user -> {
                    task.setAssignee(user);
                    taskRepository.save(task);
                    notificationUseCase.notifyTaskAssigned(task, user, null);
                    taskNotificationPort.broadcastBoardUpdate(taskMapper.toDTO(task));
                    executed(rule, task, "Assigned " + user.getFullName() + " on entering " + targetStage.getName());
                });
            }
        }
    }

    public void onTaskBlocked(Task task) {
        if (task.getPriority() != TaskPriority.CRITICAL) return;
        for (AutomationRule rule : rules(task, AutomationTrigger.TASK_BLOCKED)) {
            if (rule.getActionType() == AutomationAction.NOTIFY_MANAGER) {
                String role = rule.getTargetRole() != null ? normalizeRole(rule.getTargetRole()) : "REPORTING_MANAGER";
                int sent = notificationUseCase.notifyRolesForTask(task, Set.of(role),
                        "Escalation: critical task blocked - " + task.getTitle(),
                        "Automation '" + rule.getName() + "': '" + task.getTitle() + "' is blocked. Reason: "
                                + (task.getBlockerReason() != null ? task.getBlockerReason() : "not specified"),
                        NotificationType.BLOCKER_ALERT, NotificationPriority.CRITICAL, null, "SyncFlow Automation");
                executed(rule, task, "Escalated blocker to " + sent + " " + role + " user(s)");
            }
        }
    }

    /** Call after a checklist item was completed. */
    public void onSubtaskCompleted(Task task) {
        long total = subtaskRepository.countByTaskId(task.getId());
        if (total == 0 || subtaskRepository.countByTaskIdAndIsCompletedTrue(task.getId()) < total) return;
        if (TaskMapper.isDone(task)) return;

        for (AutomationRule rule : rules(task, AutomationTrigger.ALL_SUBTASKS_COMPLETE)) {
            if (rule.getActionType() != AutomationAction.MOVE_TO_DONE) continue;
            Optional<Stage> doneStage = stageRepositoryPort.findByProjectIdOrderByPositionAsc(task.getProject().getId())
                    .stream()
                    .filter(s -> Boolean.TRUE.equals(s.getIsDoneStage()))
                    .findFirst();
            if (doneStage.isEmpty()) continue;

            WorkflowGateValidationResultDTO gate = workflowGateService.evaluate(task, doneStage.get());
            if (!Boolean.TRUE.equals(gate.getAllowed())) {
                auditLogUseCase.recordLog("AUTOMATION_SKIPPED", "TASK", task.getId(), task.getTitle(), null, null, null,
                        "Rule '" + rule.getName() + "' did not move the task: " + String.join("; ", gate.getViolations()));
                continue;
            }

            task.setStage(doneStage.get());
            task.setProgressPercentage(100);
            task.setIsBlocked(false);
            task.setBlockerReason(null);
            task.setCompletedAt(Instant.now());
            taskRepository.save(task);

            TaskDTO dto = taskMapper.toDTO(task);
            notificationUseCase.notifyTaskCompleted(task, null);
            eventPublisher.publishEvent(ProjectEvent.of(task.getProject().getId(), ProjectEvent.TASK_MOVED, dto));
            taskNotificationPort.broadcastBoardUpdate(dto);
            executed(rule, task, "Moved to " + doneStage.get().getName() + " after checklist completion");
            return;
        }
    }

    public void onSprintClosed(Sprint sprint, String operator) {
        List<AutomationRule> rules = automationRuleRepository
                .findByProjectIdAndTriggerEventAndIsEnabledTrue(sprint.getProjectId(), AutomationTrigger.SPRINT_CLOSED);
        for (AutomationRule rule : rules) {
            if (rule.getActionType() != AutomationAction.ARCHIVE_DONE_TASKS) continue;
            int archived = 0;
            for (Task task : taskRepository.findBySprintId(sprint.getId())) {
                if (TaskMapper.isDone(task) && !Boolean.TRUE.equals(task.getIsArchived())) {
                    task.setIsArchived(true);
                    task.setArchivedAt(Instant.now());
                    task.setArchivedBy(operator);
                    taskRepository.save(task);
                    taskNotificationPort.broadcastBoardUpdate(taskMapper.toDTO(task));
                    archived++;
                }
            }
            recordExecution(rule);
            auditLogUseCase.recordLog("AUTOMATION_EXECUTED", "SPRINT", sprint.getId(), sprint.getName(), null, null, null,
                    "Rule '" + rule.getName() + "' archived " + archived + " finished task(s)");
        }
    }

    private Optional<User> assignLeastLoaded(Task task, String roleName) {
        Long projectId = task.getProject().getId();
        List<User> candidates = userRepository.findByRoleNameAndStatus(roleName, UserStatus.ACTIVE).stream()
                .filter(u -> !Boolean.TRUE.equals(u.getOnLeave()))
                .filter(u -> projectRepository.isAccessibleBy(projectId, u.getId()))
                .toList();
        if (candidates.isEmpty()) return Optional.empty();

        Map<java.util.UUID, Long> openLoad = taskRepository.findByProjectIdAndIsArchivedFalse(projectId).stream()
                .filter(t -> t.getAssignee() != null && !TaskMapper.isDone(t))
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId(), Collectors.counting()));
        return candidates.stream()
                .min(Comparator.comparing((Function<User, Long>) u -> openLoad.getOrDefault(u.getId(), 0L))
                        .thenComparing(User::getFullName));
    }

    private List<AutomationRule> rules(Task task, AutomationTrigger trigger) {
        if (task.getProject() == null) return List.of();
        return automationRuleRepository.findByProjectIdAndTriggerEventAndIsEnabledTrue(task.getProject().getId(), trigger);
    }

    private void executed(AutomationRule rule, Task task, String detail) {
        recordExecution(rule);
        auditLogUseCase.recordLog("AUTOMATION_EXECUTED", "TASK", task.getId(), task.getTitle(), null, null, null,
                "Rule '" + rule.getName() + "': " + detail);
        log.info("Automation rule {} executed on task {}: {}", rule.getId(), task.getId(), detail);
    }

    private void recordExecution(AutomationRule rule) {
        rule.setExecutionCount((rule.getExecutionCount() != null ? rule.getExecutionCount() : 0) + 1);
        rule.setLastExecutedAt(Instant.now());
        automationRuleRepository.save(rule);
    }

    private static boolean isReviewStage(Stage stage) {
        if (Boolean.TRUE.equals(stage.getRequiresChecklistComplete())) return true;
        String name = stage.getName() != null ? stage.getName().toLowerCase(Locale.ROOT) : "";
        return name.contains("review") || name.contains("qa");
    }

    /** Rules may store either "SQA" or "ROLE_SQA". */
    private static String normalizeRole(String role) {
        String r = role.trim().toUpperCase(Locale.ROOT);
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }
}
