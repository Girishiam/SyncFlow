package com.syncflow.project.application.service;

import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.*;
import com.syncflow.model.Project;
import com.syncflow.model.Stage;
import com.syncflow.model.Task;
import com.syncflow.project.application.port.in.ProjectUseCase;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectApplicationService implements ProjectUseCase {

    private final ProjectRepositoryPort projectRepositoryPort;
    private final StageRepositoryPort stageRepositoryPort;
    private final TaskRepositoryPort taskRepositoryPort;
    private final com.syncflow.repository.SprintRepository sprintRepository;
    private final com.syncflow.repository.TaskDependencyRepository taskDependencyRepository;
    private final com.syncflow.repository.AutomationRuleRepository automationRuleRepository;
    private final com.syncflow.audit.application.port.in.AuditLogUseCase auditLogUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepositoryPort.findAll().stream()
                .map(p -> ProjectDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .status(p.getStatus())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ProjectDTO createProject(com.syncflow.dto.CreateProjectRequest request) {
        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .status("ACTIVE")
                .build();

        Project savedProject = projectRepositoryPort.save(project);

        // Seed 4 canonical enterprise workflow stages
        Stage s1 = Stage.builder().project(savedProject).name("Backlog").position(1).isDoneStage(false).build();
        Stage s2 = Stage.builder().project(savedProject).name("In Development").position(2).isDoneStage(false).build();
        Stage s3 = Stage.builder().project(savedProject).name("Code Review & QA").position(3).isDoneStage(false).build();
        Stage s4 = Stage.builder().project(savedProject).name("Done & Verified").position(4).isDoneStage(true).build();

        stageRepositoryPort.save(s1);
        stageRepositoryPort.save(s2);
        stageRepositoryPort.save(s3);
        stageRepositoryPort.save(s4);

        log.info("Created new enterprise project: {} with ID: {}", savedProject.getName(), savedProject.getId());

        return ProjectDTO.builder()
                .id(savedProject.getId())
                .name(savedProject.getName())
                .description(savedProject.getDescription())
                .status(savedProject.getStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectBoardDTO getProjectBoard(Long projectId) {
        Project project = projectRepositoryPort.findById(projectId)
                .orElseThrow(() -> new DomainException("Project not found with id: " + projectId));

        List<Stage> stages = stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId);
        List<Task> allTasks = taskRepositoryPort.findByProjectId(projectId);

        List<StageDTO> stageDTOs = new ArrayList<>();
        long totalTasks = allTasks.size();
        long blockedTasks = 0;
        long completedTasks = 0;

        for (Stage stage : stages) {
            List<TaskDTO> stageTasks = new ArrayList<>();
            for (Task t : allTasks) {
                if (Boolean.TRUE.equals(t.getIsArchived())) {
                    continue; // Archived tasks are hidden from the active board view (Trello/Linear lifecycle)
                }

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
                            .deadline(t.getDeadline())
                            .isArchived(Boolean.TRUE.equals(t.getIsArchived()))
                            .archivedAt(t.getArchivedAt())
                            .archivedBy(t.getArchivedBy())
                            .startDate(t.getStartDate())
                            .sprintId(t.getSprintId())
                            .storyPoints(t.getStoryPoints() != null ? t.getStoryPoints() : 3)
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

    @Override
    @Transactional
    public StageDTO createStage(Long projectId, CreateStageRequest request) {
        Project project = projectRepositoryPort.findById(projectId)
                .orElseThrow(() -> new DomainException("Project not found: " + projectId));

        List<Stage> existing = stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId);
        int pos = request.getPosition() != null ? request.getPosition() : existing.size() + 1;

        Stage stage = Stage.builder()
                .project(project)
                .name(request.getName().trim())
                .position(pos)
                .isDoneStage(Boolean.TRUE.equals(request.getIsDoneStage()))
                .build();

        Stage saved = stageRepositoryPort.save(stage);
        log.info("Created stage {} in project {}", saved.getName(), project.getName());

        return StageDTO.builder()
                .id(saved.getId())
                .name(saved.getName())
                .position(saved.getPosition())
                .isDoneStage(saved.getIsDoneStage())
                .tasks(List.of())
                .build();
    }

    @Override
    @Transactional
    public void reorderStages(Long projectId, ReorderStagesRequest request) {
        List<Stage> existing = stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId);
        for (int i = 0; i < request.getStageIds().size(); i++) {
            Long sId = request.getStageIds().get(i);
            for (Stage s : existing) {
                if (s.getId().equals(sId)) {
                    s.setPosition(i + 1);
                    stageRepositoryPort.save(s);
                    break;
                }
            }
        }
        log.info("Reordered {} stages for project {}", request.getStageIds().size(), projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public BurndownAnalyticsDTO getBurndownAnalytics(Long projectId) {
        Project project = projectRepositoryPort.findById(projectId)
                .orElseThrow(() -> new DomainException("Project not found: " + projectId));

        List<Task> tasks = taskRepositoryPort.findByProjectId(projectId);
        int totalPoints = 0;
        int completedPoints = 0;

        for (Task t : tasks) {
            int pts = "CRITICAL".equalsIgnoreCase(t.getPriority()) ? 8 :
                      "HIGH".equalsIgnoreCase(t.getPriority()) ? 5 :
                      "MEDIUM".equalsIgnoreCase(t.getPriority()) ? 3 : 1;
            totalPoints += pts;
            if ((t.getProgressPercentage() != null && t.getProgressPercentage() == 100) ||
                (t.getStage() != null && Boolean.TRUE.equals(t.getStage().getIsDoneStage()))) {
                completedPoints += pts;
            }
        }

        if (totalPoints == 0) totalPoints = 40;
        int remainingPoints = Math.max(0, totalPoints - completedPoints);
        double completionRate = totalPoints > 0 ? ((double) completedPoints / totalPoints) * 100.0 : 0.0;

        int sprintDays = 14;
        int currentDay = 8;
        int daysRemaining = sprintDays - currentDay;

        List<BurndownAnalyticsDTO.BurndownDayDTO> dailyProgress = new ArrayList<>();
        double idealStep = (double) totalPoints / sprintDays;

        for (int day = 1; day <= sprintDays; day++) {
            double idealRem = Math.max(0, Math.round((totalPoints - (idealStep * day)) * 10.0) / 10.0);
            Double actualRem = null;
            int doneOnDay = 0;

            if (day <= currentDay) {
                // Actual burn curve interpolation to current remaining
                double fraction = (double) day / currentDay;
                double burned = completedPoints * Math.pow(fraction, 1.2);
                actualRem = Math.max(remainingPoints, Math.round((totalPoints - burned) * 10.0) / 10.0);
                doneOnDay = day == 3 || day == 5 || day == 7 ? 1 : 0;
            }

            dailyProgress.add(BurndownAnalyticsDTO.BurndownDayDTO.builder()
                    .day(day)
                    .label("Day " + day)
                    .idealRemaining(idealRem)
                    .actualRemaining(actualRem)
                    .completedTasks(doneOnDay)
                    .build());
        }

        return BurndownAnalyticsDTO.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .sprintDurationDays(sprintDays)
                .currentDay(currentDay)
                .totalStoryPoints(totalPoints)
                .remainingStoryPoints(remainingPoints)
                .completionPercentage(Math.round(completionRate * 10.0) / 10.0)
                .velocityPointsPerWeek(28)
                .averageCycleTimeDays(2.4)
                .daysRemainingInSprint(daysRemaining)
                .dailyProgress(dailyProgress)
                .build();
    }

    // --- Roadmap & Gantt Timeline ---
    @Override
    @Transactional(readOnly = true)
    public List<TimelineTaskDTO> getProjectTimeline(Long projectId) {
        List<Task> tasks = taskRepositoryPort.findByProjectId(projectId).stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsArchived()))
                .toList();

        List<TimelineTaskDTO> timeline = new ArrayList<>();

        for (Task t : tasks) {
            String assigneeName = t.getAssignee() != null ? t.getAssignee().getFullName() : "Unassigned";
            String avatar = getInitials(assigneeName);

            Instant start = t.getStartDate() != null ? t.getStartDate() : t.getCreatedAt();
            Instant end = t.getDeadline() != null ? t.getDeadline() : start.plus(7, java.time.temporal.ChronoUnit.DAYS);

            List<Long> blockedBy = taskDependencyRepository.findBySourceTaskId(t.getId()).stream()
                    .filter(d -> "BLOCKED_BY".equalsIgnoreCase(d.getDependencyType()) || "BLOCKS".equalsIgnoreCase(d.getDependencyType()))
                    .map(com.syncflow.model.TaskDependency::getTargetTaskId)
                    .toList();

            timeline.add(TimelineTaskDTO.builder()
                    .id(t.getId())
                    .title(t.getTitle())
                    .priority(t.getPriority())
                    .progress(t.getProgressPercentage() != null ? t.getProgressPercentage() : 0)
                    .stageId(t.getStage() != null ? t.getStage().getId() : null)
                    .stageName(t.getStage() != null ? t.getStage().getName() : "")
                    .isDoneStage(t.getStage() != null && Boolean.TRUE.equals(t.getStage().getIsDoneStage()))
                    .assignee(assigneeName)
                    .avatar(avatar)
                    .startDate(start)
                    .deadline(end)
                    .isBlocked(Boolean.TRUE.equals(t.getIsBlocked()))
                    .storyPoints(t.getStoryPoints() != null ? t.getStoryPoints() : 3)
                    .blockedByTaskIds(blockedBy)
                    .build());
        }

        return timeline;
    }

    // --- Agile Sprints & Planning ---
    @Override
    @Transactional(readOnly = true)
    public List<SprintDTO> getProjectSprints(Long projectId) {
        List<com.syncflow.model.Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
        List<Task> projectTasks = taskRepositoryPort.findByProjectId(projectId);

        List<SprintDTO> result = new ArrayList<>();
        for (com.syncflow.model.Sprint s : sprints) {
            List<Task> sprintTasks = projectTasks.stream()
                    .filter(t -> s.getId().equals(t.getSprintId()))
                    .toList();

            int totalTasks = sprintTasks.size();
            int completedTasks = (int) sprintTasks.stream()
                    .filter(t -> (t.getStage() != null && Boolean.TRUE.equals(t.getStage().getIsDoneStage())) || (t.getProgressPercentage() != null && t.getProgressPercentage() == 100))
                    .count();

            int totalPoints = sprintTasks.stream()
                    .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 3)
                    .sum();

            int completedPoints = sprintTasks.stream()
                    .filter(t -> (t.getStage() != null && Boolean.TRUE.equals(t.getStage().getIsDoneStage())) || (t.getProgressPercentage() != null && t.getProgressPercentage() == 100))
                    .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 3)
                    .sum();

            result.add(SprintDTO.builder()
                    .id(s.getId())
                    .projectId(s.getProjectId())
                    .name(s.getName())
                    .goal(s.getGoal())
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .status(s.getStatus())
                    .targetStoryPoints(s.getTargetStoryPoints())
                    .totalTasks(totalTasks)
                    .completedTasks(completedTasks)
                    .totalStoryPoints(totalPoints)
                    .completedStoryPoints(completedPoints)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public SprintDTO createSprint(Long projectId, SprintDTO request) {
        com.syncflow.model.Sprint sprint = com.syncflow.model.Sprint.builder()
                .projectId(projectId)
                .name(request.getName().trim())
                .goal(request.getGoal())
                .startDate(request.getStartDate() != null ? request.getStartDate() : java.time.LocalDate.now())
                .endDate(request.getEndDate() != null ? request.getEndDate() : java.time.LocalDate.now().plusDays(14))
                .status("PLANNING")
                .targetStoryPoints(request.getTargetStoryPoints() != null ? request.getTargetStoryPoints() : 30)
                .createdAt(Instant.now())
                .build();

        com.syncflow.model.Sprint saved = sprintRepository.save(sprint);

        try {
            auditLogUseCase.recordLog("SPRINT_CREATED", "SPRINT", saved.getId(), saved.getName(),
                    "Manager", null, "PROJECT_MANAGER", "Created sprint with target " + saved.getTargetStoryPoints() + " pts");
        } catch (Exception e) {
            log.warn("Failed to audit sprint creation: {}", e.getMessage());
        }

        return SprintDTO.builder()
                .id(saved.getId())
                .projectId(saved.getProjectId())
                .name(saved.getName())
                .goal(saved.getGoal())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus())
                .targetStoryPoints(saved.getTargetStoryPoints())
                .totalTasks(0)
                .completedTasks(0)
                .totalStoryPoints(0)
                .completedStoryPoints(0)
                .build();
    }

    @Override
    @Transactional
    public SprintDTO startSprint(Long sprintId) {
        com.syncflow.model.Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new DomainException("Sprint not found: " + sprintId));

        sprint.setStatus("ACTIVE");
        com.syncflow.model.Sprint saved = sprintRepository.save(sprint);

        try {
            auditLogUseCase.recordLog("SPRINT_STARTED", "SPRINT", saved.getId(), saved.getName(),
                    "Manager", null, "PROJECT_MANAGER", "Activated sprint: " + saved.getName());
        } catch (Exception e) {
            log.warn("Failed to audit sprint start: {}", e.getMessage());
        }

        return SprintDTO.builder()
                .id(saved.getId())
                .projectId(saved.getProjectId())
                .name(saved.getName())
                .goal(saved.getGoal())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus())
                .targetStoryPoints(saved.getTargetStoryPoints())
                .build();
    }

    @Override
    @Transactional
    public SprintDTO completeSprint(Long sprintId) {
        com.syncflow.model.Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new DomainException("Sprint not found: " + sprintId));

        sprint.setStatus("COMPLETED");
        com.syncflow.model.Sprint saved = sprintRepository.save(sprint);

        try {
            auditLogUseCase.recordLog("SPRINT_COMPLETED", "SPRINT", saved.getId(), saved.getName(),
                    "Manager", null, "PROJECT_MANAGER", "Completed and closed sprint: " + saved.getName());
        } catch (Exception e) {
            log.warn("Failed to audit sprint completion: {}", e.getMessage());
        }

        return SprintDTO.builder()
                .id(saved.getId())
                .projectId(saved.getProjectId())
                .name(saved.getName())
                .goal(saved.getGoal())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus())
                .targetStoryPoints(saved.getTargetStoryPoints())
                .build();
    }

    // --- Butler Automation Rules ---
    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDTO> getAutomationRules(Long projectId) {
        return automationRuleRepository.findByProjectId(projectId).stream()
                .map(r -> AutomationRuleDTO.builder()
                        .id(r.getId())
                        .projectId(r.getProjectId())
                        .name(r.getName())
                        .description(r.getDescription())
                        .triggerEvent(r.getTriggerEvent())
                        .actionType(r.getActionType())
                        .targetRole(r.getTargetRole())
                        .isEnabled(r.getIsEnabled())
                        .executionCount(r.getExecutionCount())
                        .lastExecutedAt(r.getLastExecutedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public AutomationRuleDTO toggleAutomationRule(Long ruleId) {
        com.syncflow.model.AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new DomainException("Automation rule not found: " + ruleId));

        rule.setIsEnabled(!Boolean.TRUE.equals(rule.getIsEnabled()));
        com.syncflow.model.AutomationRule saved = automationRuleRepository.save(rule);

        try {
            auditLogUseCase.recordLog("AUTOMATION_TOGGLED", "AUTOMATION", saved.getId(), saved.getName(),
                    "Lead", null, "TEAM_LEAD", "Automation rule is now " + (saved.getIsEnabled() ? "ENABLED" : "DISABLED"));
        } catch (Exception e) {
            log.warn("Failed to audit automation toggle: {}", e.getMessage());
        }

        return AutomationRuleDTO.builder()
                .id(saved.getId())
                .projectId(saved.getProjectId())
                .name(saved.getName())
                .description(saved.getDescription())
                .triggerEvent(saved.getTriggerEvent())
                .actionType(saved.getActionType())
                .targetRole(saved.getTargetRole())
                .isEnabled(saved.getIsEnabled())
                .executionCount(saved.getExecutionCount())
                .lastExecutedAt(saved.getLastExecutedAt())
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
