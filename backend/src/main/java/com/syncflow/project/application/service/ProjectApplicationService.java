package com.syncflow.project.application.service;

import com.syncflow.project.application.port.out.WebhookDeliveryPort;

import com.syncflow.project.application.port.out.SprintRepositoryPort;
import com.syncflow.project.application.port.out.AutomationRuleRepositoryPort;
import com.syncflow.project.application.port.out.WebhookSubscriptionRepositoryPort;
import com.syncflow.task.application.port.out.TaskDependencyRepositoryPort;
import com.syncflow.task.application.port.out.TaskTimeLogRepositoryPort;
import com.syncflow.task.application.port.out.TaskRepositoryPort;

import com.syncflow.model.enums.*;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.automation.application.AutomationEngine;
import com.syncflow.common.domain.ConflictException;
import com.syncflow.common.domain.DomainException;
import com.syncflow.common.domain.NotFoundException;
import com.syncflow.common.event.ProjectEvent;
import com.syncflow.config.SyncFlowProperties;
import com.syncflow.dto.*;
import com.syncflow.model.*;
import com.syncflow.notification.application.port.in.NotificationUseCase;
import com.syncflow.project.application.port.in.ProjectUseCase;
import com.syncflow.project.application.port.out.ProjectRepositoryPort;
import com.syncflow.project.application.port.out.StageRepositoryPort;
import com.syncflow.security.CurrentUser;
import com.syncflow.security.UserDetailsImpl;
import com.syncflow.task.application.port.out.TaskNotificationPort;
import com.syncflow.task.application.service.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectApplicationService implements ProjectUseCase {

    private static final int FALLBACK_WINDOW_DAYS = 14;
    private static final int CYCLE_TIME_LOOKBACK_DAYS = 90;

    private final ProjectRepositoryPort projectRepositoryPort;
    private final StageRepositoryPort stageRepositoryPort;
    private final TaskRepositoryPort taskRepository;
    private final SprintRepositoryPort sprintRepository;
    private final TaskDependencyRepositoryPort taskDependencyRepository;
    private final AutomationRuleRepositoryPort automationRuleRepository;
    private final AuditLogUseCase auditLogUseCase;
    private final WebhookSubscriptionRepositoryPort webhookSubscriptionRepository;
    private final WebhookDeliveryPort webhookDelivery;
    private final UserRepositoryPort userRepositoryPort;
    private final TaskTimeLogRepositoryPort taskTimeLogRepository;
    private final TaskMapper taskMapper;
    private final TaskNotificationPort taskNotificationPort;
    private final AutomationEngine automationEngine;
    private final NotificationUseCase notificationUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUser currentUser;
    private final SyncFlowProperties properties;

    // ---------------------------------------------------------------- projects & stages

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        UserDetailsImpl user = currentUser.require();
        List<Project> projects = CurrentUser.isAdmin(user)
                ? projectRepositoryPort.findAll()
                : projectRepositoryPort.findAccessibleBy(user.getId());
        return projects.stream().map(this::toProjectDTO).toList();
    }

    @Override
    @Transactional
    public ProjectDTO createProject(CreateProjectRequest request) {
        User manager = currentUser.get().flatMap(u -> userRepositoryPort.findById(u.getId())).orElse(null);
        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDate.now())) {
            throw new DomainException("The due date cannot be in the past");
        }
        Project savedProject = projectRepositoryPort.save(Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.now())
                .dueDate(request.getDueDate())
                .manager(manager)
                .totalBudget(request.getTotalBudget())
                .allocatedBudget(request.getAllocatedBudget())
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank()
                        ? request.getCurrency().trim().toUpperCase(Locale.ROOT) : "USD")
                .build());

        // Default workflow with its quality gates
        stageRepositoryPort.save(Stage.builder().project(savedProject).name("Backlog").position(1).build());
        stageRepositoryPort.save(Stage.builder().project(savedProject).name("In Development").position(2).build());
        stageRepositoryPort.save(Stage.builder().project(savedProject).name("Code Review & QA").position(3)
                .requiresChecklistComplete(true).build());
        stageRepositoryPort.save(Stage.builder().project(savedProject).name("Done & Verified").position(4)
                .isDoneStage(true).requiresDeliverable(true).requiresNoBlockers(true).build());

        // The creator manages the project; everyone else joins with the role they were given.
        if (manager != null) {
            projectRepositoryPort.saveMember(ProjectMember.builder()
                    .projectId(savedProject.getId()).user(manager).role(ProjectRole.MANAGER).build());
        }
        int added = 0;
        for (ProjectMemberRequest m : request.getMembers()) {
            if (m.getUserId() == null || (manager != null && manager.getId().equals(m.getUserId()))) continue;
            if (projectRepositoryPort.findMember(savedProject.getId(), m.getUserId()).isPresent()) continue;
            User user = requireActiveUser(m.getUserId());
            projectRepositoryPort.saveMember(ProjectMember.builder()
                    .projectId(savedProject.getId()).user(user).role(parseProjectRole(m.getRole())).build());
            notificationUseCase.notifyAddedToProject(savedProject, user, manager);
            added++;
        }

        auditLogUseCase.recordLog("PROJECT_CREATED", "PROJECT", savedProject.getId(), savedProject.getName(),
                null, null, null, "Created project with default 4-stage workflow and " + (added + 1) + " team member(s)");
        return toProjectDTO(savedProject);
    }

    // ---------------------------------------------------------------- project team

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberDTO> getMembers(Long projectId) {
        requireProject(projectId);
        Map<UUID, List<Task>> openByAssignee = taskRepository.findByProjectIdAndIsArchivedFalse(projectId).stream()
                .filter(t -> t.getAssignee() != null && !TaskMapper.isDone(t))
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId()));
        return projectRepositoryPort.findMembers(projectId).stream()
                .map(m -> toMemberDTO(m, openByAssignee.getOrDefault(m.getUser().getId(), List.of())))
                .sorted(Comparator.comparing((ProjectMemberDTO d) -> ProjectRole.valueOf(d.getRole()).ordinal())
                        .thenComparing(ProjectMemberDTO::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional
    public ProjectMemberDTO addMember(Long projectId, ProjectMemberRequest request) {
        Project project = requireProject(projectId);
        if (request.getUserId() == null) {
            throw new DomainException("Choose a person to add");
        }
        if (projectRepositoryPort.findMember(projectId, request.getUserId()).isPresent()) {
            throw new ConflictException("That person is already on the team");
        }
        User user = requireActiveUser(request.getUserId());
        ProjectRole role = parseProjectRole(request.getRole());
        ProjectMember saved = projectRepositoryPort.saveMember(ProjectMember.builder()
                .projectId(projectId).user(user).role(role).build());

        User actor = actorEntity();
        notificationUseCase.notifyAddedToProject(project, user, actor);
        auditLogUseCase.recordLog("PROJECT_MEMBER_ADDED", "PROJECT", projectId, project.getName(),
                null, null, role.name(), "Added " + user.getFullName() + " as " + role.name().toLowerCase(Locale.ROOT));
        return toMemberDTO(saved, List.of());
    }

    @Override
    @Transactional
    public ProjectMemberDTO updateMemberRole(Long projectId, UUID userId, ProjectMemberRequest request) {
        Project project = requireProject(projectId);
        ProjectMember member = requireMember(projectId, userId);
        ProjectRole newRole = parseProjectRole(request.getRole());
        ProjectRole oldRole = member.getRole();
        if (oldRole == newRole) {
            return toMemberDTO(member, List.of());
        }
        if (oldRole == ProjectRole.MANAGER && newRole != ProjectRole.MANAGER && managerCount(projectId) <= 1) {
            throw new ConflictException("A project needs at least one manager. Make someone else a manager first.");
        }
        member.setRole(newRole);
        ProjectMember saved = projectRepositoryPort.saveMember(member);
        auditLogUseCase.recordLog("PROJECT_MEMBER_ROLE_CHANGED", "PROJECT", projectId, project.getName(),
                "role", oldRole.name(), newRole.name(), member.getUser().getFullName() + " is now " + newRole.name().toLowerCase(Locale.ROOT));
        return toMemberDTO(saved, List.of());
    }

    /** Removes someone from the team; their unfinished tasks in this project become unassigned. */
    @Override
    @Transactional
    public int removeMember(Long projectId, UUID userId) {
        Project project = requireProject(projectId);
        ProjectMember member = requireMember(projectId, userId);
        if (member.getRole() == ProjectRole.MANAGER && managerCount(projectId) <= 1) {
            throw new ConflictException("A project needs at least one manager. Make someone else a manager first.");
        }

        int unassigned = 0;
        for (Task task : taskRepository.findByProjectIdAndIsArchivedFalse(projectId)) {
            if (task.getAssignee() != null && task.getAssignee().getId().equals(userId) && !TaskMapper.isDone(task)) {
                task.setAssignee(null);
                taskRepository.save(task);
                taskNotificationPort.broadcastBoardUpdate(taskMapper.toDTO(task));
                unassigned++;
            }
        }
        projectRepositoryPort.deleteMember(member);
        auditLogUseCase.recordLog("PROJECT_MEMBER_REMOVED", "PROJECT", projectId, project.getName(),
                null, member.getRole().name(), null,
                "Removed " + member.getUser().getFullName() + " from the team; " + unassigned + " open task(s) unassigned");
        return unassigned;
    }

    private long managerCount(Long projectId) {
        return projectRepositoryPort.findMembers(projectId).stream()
                .filter(m -> m.getRole() == ProjectRole.MANAGER).count();
    }

    private ProjectMember requireMember(Long projectId, UUID userId) {
        return projectRepositoryPort.findMember(projectId, userId)
                .orElseThrow(() -> new NotFoundException("That person is not on this project"));
    }

    private User requireActiveUser(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException(user.getFullName() + " is not an active user");
        }
        return user;
    }

    private User actorEntity() {
        return currentUser.get().flatMap(u -> userRepositoryPort.findById(u.getId())).orElse(null);
    }

    private static ProjectRole parseProjectRole(String role) {
        if (role == null || role.isBlank()) return ProjectRole.MEMBER;
        try {
            return ProjectRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DomainException("Unknown project role: " + role + ". Use MANAGER, LEAD or MEMBER.");
        }
    }

    private ProjectMemberDTO toMemberDTO(ProjectMember m, List<Task> openTasks) {
        User u = m.getUser();
        return ProjectMemberDTO.builder()
                .userId(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .avatar(TaskMapper.initials(u.getFullName()))
                .systemRole(u.getRole() != null ? u.getRole().getName() : null)
                .role(m.getRole().name())
                .onLeave(Boolean.TRUE.equals(u.getOnLeave()))
                .openPoints(openTasks.stream().mapToInt(this::points).sum())
                .openTasks(openTasks.size())
                .addedAt(m.getAddedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectBoardDTO getProjectBoard(Long projectId) {
        Project project = requireProject(projectId);
        List<Stage> stages = stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId);
        List<Task> activeTasks = taskRepository.findByProjectIdAndIsArchivedFalse(projectId);

        Map<Long, List<TaskDTO>> byStage = taskMapper.toDTOs(activeTasks).stream()
                .filter(t -> t.getStageId() != null)
                .collect(Collectors.groupingBy(TaskDTO::getStageId, LinkedHashMap::new, Collectors.toList()));

        long blocked = activeTasks.stream().filter(t -> Boolean.TRUE.equals(t.getIsBlocked())).count();
        long completed = activeTasks.stream().filter(TaskMapper::isDone).count();
        int completionPercentage = activeTasks.isEmpty() ? 0 : (int) Math.round(100.0 * completed / activeTasks.size());

        List<StageDTO> stageDTOs = stages.stream()
                .map(s -> toStageDTO(s, byStage.getOrDefault(s.getId(), List.of())))
                .toList();

        return ProjectBoardDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus().name())
                .stages(stageDTOs)
                .totalTasks(activeTasks.size())
                .blockedTasks(blocked)
                .completionPercentage(completionPercentage)
                .build();
    }

    @Override
    @Transactional
    public StageDTO createStage(Long projectId, CreateStageRequest request) {
        Project project = requireProject(projectId);
        List<Stage> existing = stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId);
        boolean isDone = Boolean.TRUE.equals(request.getIsDoneStage());

        Stage saved = stageRepositoryPort.save(Stage.builder()
                .project(project)
                .name(request.getName().trim())
                .position(request.getPosition() != null ? request.getPosition() : existing.size() + 1)
                .isDoneStage(isDone)
                .requiresChecklistComplete(Boolean.TRUE.equals(request.getRequiresChecklistComplete()))
                .requiresDeliverable(request.getRequiresDeliverable() != null ? request.getRequiresDeliverable() : isDone)
                .requiresNoBlockers(request.getRequiresNoBlockers() != null ? request.getRequiresNoBlockers() : isDone)
                .build());
        auditLogUseCase.recordLog("STAGE_CREATED", "PROJECT", projectId, project.getName(), null, null, null,
                "Added stage '" + saved.getName() + "'");
        return toStageDTO(saved, List.of());
    }

    @Override
    @Transactional
    public StageDTO updateStage(Long stageId, UpdateStageRequest request) {
        Stage stage = stageRepositoryPort.findById(stageId)
                .orElseThrow(() -> new NotFoundException("Stage not found: " + stageId));
        if (request.getName() != null) stage.setName(request.getName().trim());
        if (request.getIsDoneStage() != null) stage.setIsDoneStage(request.getIsDoneStage());
        if (request.getRequiresChecklistComplete() != null) stage.setRequiresChecklistComplete(request.getRequiresChecklistComplete());
        if (request.getRequiresDeliverable() != null) stage.setRequiresDeliverable(request.getRequiresDeliverable());
        if (request.getRequiresNoBlockers() != null) stage.setRequiresNoBlockers(request.getRequiresNoBlockers());
        Stage saved = stageRepositoryPort.save(stage);

        auditLogUseCase.recordLog("STAGE_UPDATED", "PROJECT", saved.getProject().getId(), saved.getName(), null, null, null,
                String.format("Gates: checklist=%s, deliverable=%s, noBlockers=%s, done=%s",
                        saved.getRequiresChecklistComplete(), saved.getRequiresDeliverable(),
                        saved.getRequiresNoBlockers(), saved.getIsDoneStage()));
        return toStageDTO(saved, List.of());
    }

    @Override
    @Transactional
    public void reorderStages(Long projectId, ReorderStagesRequest request) {
        List<Stage> existing = stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId);
        Map<Long, Stage> byId = existing.stream().collect(Collectors.toMap(Stage::getId, s -> s));
        List<Long> ids = request.getStageIds() != null ? request.getStageIds() : List.of();
        if (ids.size() != existing.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new DomainException("Reorder must list every stage of the project exactly once");
        }
        for (int i = 0; i < ids.size(); i++) {
            Stage stage = byId.get(ids.get(i));
            stage.setPosition(i + 1);
            stageRepositoryPort.save(stage);
        }
    }

    // ---------------------------------------------------------------- burndown & velocity

    /**
     * Burndown for the active sprint (or the most recently started one). Remaining points on each past day are
     * derived from when tasks actually entered a done stage ({@link Task#getCompletedAt()}).
     * Without any sprint, the last 14 days of the whole project are charted.
     */
    @Override
    @Transactional(readOnly = true)
    public BurndownAnalyticsDTO getBurndownAnalytics(Long projectId) {
        Project project = requireProject(projectId);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        Optional<Sprint> sprint = sprintRepository.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
                .or(() -> sprintRepository.findByProjectIdOrderByStartDateDesc(projectId).stream()
                        .filter(s -> s.getStartDate() != null && !s.getStartDate().isAfter(today))
                        .findFirst());

        LocalDate start;
        LocalDate end;
        List<Task> scope;
        if (sprint.isPresent() && sprint.get().getStartDate() != null && sprint.get().getEndDate() != null) {
            start = sprint.get().getStartDate();
            end = sprint.get().getEndDate();
            scope = taskRepository.findBySprintId(sprint.get().getId());
        } else {
            sprint = Optional.empty();
            end = today;
            start = today.minusDays(FALLBACK_WINDOW_DAYS - 1);
            scope = taskRepository.findByProjectId(projectId).stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsArchived()) || t.getCompletedAt() != null)
                    .toList();
        }

        int totalDays = (int) Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);
        int currentDay = (int) Math.max(0, Math.min(totalDays, ChronoUnit.DAYS.between(start, today) + 1));
        int totalPoints = scope.stream().mapToInt(this::points).sum();
        int completedPoints = scope.stream().filter(TaskMapper::isDone).mapToInt(this::points).sum();

        List<BurndownAnalyticsDTO.BurndownDayDTO> days = new ArrayList<>();
        for (int day = 1; day <= totalDays; day++) {
            LocalDate date = start.plusDays(day - 1);
            Instant endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant();
            Instant startOfDay = date.atStartOfDay(zone).toInstant();
            double ideal = Math.max(0, round1(totalPoints - (double) totalPoints * day / totalDays));

            Double actual = null;
            int doneThatDay = 0;
            if (day <= currentDay) {
                int burned = scope.stream()
                        .filter(t -> TaskMapper.isDone(t) && t.getCompletedAt() != null && t.getCompletedAt().isBefore(endOfDay))
                        .mapToInt(this::points).sum();
                actual = (double) Math.max(0, totalPoints - burned);
                doneThatDay = (int) scope.stream()
                        .filter(t -> t.getCompletedAt() != null && !t.getCompletedAt().isBefore(startOfDay)
                                && t.getCompletedAt().isBefore(endOfDay))
                        .count();
            }
            days.add(BurndownAnalyticsDTO.BurndownDayDTO.builder()
                    .day(day)
                    .label(date.toString())
                    .idealRemaining(ideal)
                    .actualRemaining(actual)
                    .completedTasks(doneThatDay)
                    .build());
        }

        return BurndownAnalyticsDTO.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .sprintId(sprint.map(Sprint::getId).orElse(null))
                .sprintName(sprint.map(Sprint::getName).orElse(null))
                .sprintDurationDays(totalDays)
                .currentDay(currentDay)
                .totalStoryPoints(totalPoints)
                .remainingStoryPoints(Math.max(0, totalPoints - completedPoints))
                .completionPercentage(totalPoints > 0 ? round1(100.0 * completedPoints / totalPoints) : 0.0)
                .velocityPointsPerWeek(velocityPerWeek(projectId, sprint.orElse(null), completedPoints, currentDay))
                .averageCycleTimeDays(averageCycleTimeDays(projectId))
                .daysRemainingInSprint(Math.max(0, totalDays - currentDay))
                .dailyProgress(days)
                .build();
    }

    /** Average points per week over the last three closed sprints, else the current sprint's pace. */
    private int velocityPerWeek(Long projectId, Sprint current, int currentCompletedPoints, int currentDay) {
        List<Sprint> closed = sprintRepository.findTop3ByProjectIdAndStatusOrderByEndDateDesc(projectId, SprintStatus.COMPLETED);
        double total = 0;
        double weeks = 0;
        for (Sprint s : closed) {
            if (s.getCompletedStoryPoints() == null || s.getStartDate() == null || s.getEndDate() == null) continue;
            total += s.getCompletedStoryPoints();
            weeks += (ChronoUnit.DAYS.between(s.getStartDate(), s.getEndDate()) + 1) / 7.0;
        }
        if (weeks > 0) {
            return (int) Math.round(total / weeks);
        }
        if (current != null && currentDay > 0) {
            return (int) Math.round(currentCompletedPoints / (currentDay / 7.0));
        }
        return 0;
    }

    /** Mean days from start (or creation) to completion for tasks finished in the last 90 days. */
    private double averageCycleTimeDays(Long projectId) {
        Instant cutoff = Instant.now().minus(CYCLE_TIME_LOOKBACK_DAYS, ChronoUnit.DAYS);
        return round1(taskRepository.findByProjectId(projectId).stream()
                .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(cutoff))
                .mapToDouble(t -> {
                    Instant began = t.getStartDate() != null ? t.getStartDate() : t.getCreatedAt();
                    return Math.max(0, Duration.between(began, t.getCompletedAt()).toHours() / 24.0);
                })
                .average()
                .orElse(0.0));
    }

    // ---------------------------------------------------------------- timeline

    @Override
    @Transactional(readOnly = true)
    public List<TimelineTaskDTO> getProjectTimeline(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectIdAndIsArchivedFalse(projectId);
        Set<Long> ids = tasks.stream().map(Task::getId).collect(Collectors.toSet());

        // blockedBy(t) = tasks t declares BLOCKED_BY + tasks that declare they BLOCK t
        Map<Long, Set<Long>> blockedBy = new HashMap<>();
        if (!ids.isEmpty()) {
            for (TaskDependency d : taskDependencyRepository.findBySourceTaskIdIn(ids)) {
                if (d.getDependencyType() == DependencyType.BLOCKED_BY) {
                    blockedBy.computeIfAbsent(d.getSourceTaskId(), k -> new LinkedHashSet<>()).add(d.getTargetTaskId());
                }
            }
            for (TaskDependency d : taskDependencyRepository.findByTargetTaskIdIn(ids)) {
                if (d.getDependencyType() == DependencyType.BLOCKS) {
                    blockedBy.computeIfAbsent(d.getTargetTaskId(), k -> new LinkedHashSet<>()).add(d.getSourceTaskId());
                }
            }
        }

        return tasks.stream().map(t -> {
            String assigneeName = t.getAssignee() != null ? t.getAssignee().getFullName() : "Unassigned";
            Instant start = t.getStartDate() != null ? t.getStartDate() : t.getCreatedAt();
            Instant end = t.getDeadline() != null ? t.getDeadline() : start.plus(7, ChronoUnit.DAYS);
            return TimelineTaskDTO.builder()
                    .id(t.getId())
                    .title(t.getTitle())
                    .priority(t.getPriority().name())
                    .progress(t.getProgressPercentage() != null ? t.getProgressPercentage() : 0)
                    .stageId(t.getStage() != null ? t.getStage().getId() : null)
                    .stageName(t.getStage() != null ? t.getStage().getName() : "")
                    .isDoneStage(TaskMapper.isDone(t))
                    .assignee(assigneeName)
                    .avatar(TaskMapper.initials(assigneeName))
                    .startDate(start)
                    .deadline(end)
                    .isBlocked(Boolean.TRUE.equals(t.getIsBlocked()))
                    .storyPoints(points(t))
                    .blockedByTaskIds(new ArrayList<>(blockedBy.getOrDefault(t.getId(), Set.of())))
                    .build();
        }).toList();
    }

    // ---------------------------------------------------------------- sprints

    @Override
    @Transactional(readOnly = true)
    public List<SprintDTO> getProjectSprints(Long projectId) {
        List<Sprint> sprints = sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
        Map<Long, List<Task>> tasksBySprint = taskRepository.findByProjectId(projectId).stream()
                .filter(t -> t.getSprintId() != null)
                .collect(Collectors.groupingBy(Task::getSprintId));
        return sprints.stream()
                .map(s -> toSprintDTO(s, tasksBySprint.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public SprintDTO createSprint(Long projectId, SprintDTO request) {
        requireProject(projectId);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new DomainException("Sprint name is required");
        }
        LocalDate start = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalDate end = request.getEndDate() != null ? request.getEndDate() : start.plusDays(13);
        if (end.isBefore(start)) {
            throw new DomainException("Sprint end date must not be before its start date");
        }
        if (request.getTargetStoryPoints() != null && request.getTargetStoryPoints() < 0) {
            throw new DomainException("Target story points must not be negative");
        }

        Sprint saved = sprintRepository.save(Sprint.builder()
                .projectId(projectId)
                .name(request.getName().trim())
                .goal(request.getGoal())
                .startDate(start)
                .endDate(end)
                .status(SprintStatus.PLANNING)
                .targetStoryPoints(request.getTargetStoryPoints() != null ? request.getTargetStoryPoints() : 30)
                .createdAt(Instant.now())
                .build());
        auditLogUseCase.recordLog("SPRINT_CREATED", "SPRINT", saved.getId(), saved.getName(), null, null, null,
                "Created sprint targeting " + saved.getTargetStoryPoints() + " pts");
        return toSprintDTO(saved, List.of());
    }

    @Override
    @Transactional
    public SprintDTO startSprint(Long sprintId) {
        Sprint sprint = requireSprint(sprintId);
        if (sprint.getStatus() != SprintStatus.PLANNING) {
            throw new ConflictException("Only a sprint in PLANNING can be started");
        }
        if (sprintRepository.existsByProjectIdAndStatus(sprint.getProjectId(), SprintStatus.ACTIVE)) {
            throw new ConflictException("Another sprint is already active in this project. Complete it first.");
        }
        sprint.setStatus(SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null || sprint.getStartDate().isAfter(LocalDate.now())) {
            sprint.setStartDate(LocalDate.now());
        }
        Sprint saved = sprintRepository.save(sprint);
        auditLogUseCase.recordLog("SPRINT_STARTED", "SPRINT", saved.getId(), saved.getName(), null, null, null,
                "Activated sprint");
        return toSprintDTO(saved, taskRepository.findBySprintId(sprintId));
    }

    /**
     * Closes an active sprint: records the completed points for velocity, returns unfinished tasks
     * to the backlog, runs SPRINT_CLOSED automations and emits the SPRINT_COMPLETED webhook.
     */
    @Override
    @Transactional
    public SprintDTO completeSprint(Long sprintId, CompleteSprintRequest request) {
        Sprint sprint = requireSprint(sprintId);
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new ConflictException("Only an active sprint can be completed");
        }

        Map<Long, Long> targets = new HashMap<>();
        Set<Long> checkedSprints = new HashSet<>();
        if (request != null && request.getMoves() != null) {
            for (CompleteSprintRequest.Move move : request.getMoves()) {
                if (move.getTaskId() == null) continue;
                if (move.getSprintId() != null && checkedSprints.add(move.getSprintId())) {
                    Sprint target = requireSprint(move.getSprintId());
                    if (!target.getProjectId().equals(sprint.getProjectId())) {
                        throw new DomainException("Unfinished work can only move to a sprint in the same project");
                    }
                    if (target.getStatus() != SprintStatus.PLANNING) {
                        throw new ConflictException("Unfinished work can only move to a sprint that has not started yet");
                    }
                }
                targets.put(move.getTaskId(), move.getSprintId());
            }
        }

        List<Task> sprintTasks = taskRepository.findBySprintId(sprintId);
        int completedPoints = sprintTasks.stream().filter(TaskMapper::isDone).mapToInt(this::points).sum();
        int toBacklog = 0;
        int toNextSprint = 0;
        for (Task task : sprintTasks) {
            if (!TaskMapper.isDone(task) && !Boolean.TRUE.equals(task.getIsArchived())) {
                Long target = targets.get(task.getId());
                task.setSprintId(target);
                taskRepository.save(task);
                taskNotificationPort.broadcastBoardUpdate(taskMapper.toDTO(task));
                if (target != null) toNextSprint++; else toBacklog++;
            }
        }

        sprint.setStatus(SprintStatus.COMPLETED);
        sprint.setCompletedAt(Instant.now());
        sprint.setCompletedStoryPoints(completedPoints);
        Sprint saved = sprintRepository.save(sprint);

        String operator = currentUser.get().map(UserDetailsImpl::getFullName).orElse("SyncFlow System");
        automationEngine.onSprintClosed(saved, operator);
        auditLogUseCase.recordLog("SPRINT_COMPLETED", "SPRINT", saved.getId(), saved.getName(), null, null, null,
                "Closed sprint with " + completedPoints + " pts done; " + toNextSprint + " unfinished task(s) moved to the next sprint, "
                        + toBacklog + " returned to the backlog");

        SprintDTO dto = toSprintDTO(saved, taskRepository.findBySprintId(sprintId));
        eventPublisher.publishEvent(ProjectEvent.of(saved.getProjectId(), ProjectEvent.SPRINT_COMPLETED, dto));
        return dto;
    }

    private SprintDTO toSprintDTO(Sprint s, List<Task> sprintTasks) {
        List<Task> done = sprintTasks.stream().filter(TaskMapper::isDone).toList();
        return SprintDTO.builder()
                .id(s.getId())
                .projectId(s.getProjectId())
                .name(s.getName())
                .goal(s.getGoal())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus().name())
                .targetStoryPoints(s.getTargetStoryPoints())
                .totalTasks(sprintTasks.size())
                .completedTasks(done.size())
                .totalStoryPoints(sprintTasks.stream().mapToInt(this::points).sum())
                .completedStoryPoints(s.getCompletedStoryPoints() != null
                        ? s.getCompletedStoryPoints()
                        : done.stream().mapToInt(this::points).sum())
                .build();
    }

    // ---------------------------------------------------------------- automations

    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDTO> getAutomationRules(Long projectId) {
        return automationRuleRepository.findByProjectId(projectId).stream().map(this::toRuleDTO).toList();
    }

    @Override
    @Transactional
    public AutomationRuleDTO toggleAutomationRule(Long ruleId) {
        AutomationRule rule = automationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Automation rule not found: " + ruleId));
        rule.setIsEnabled(!Boolean.TRUE.equals(rule.getIsEnabled()));
        AutomationRule saved = automationRuleRepository.save(rule);
        auditLogUseCase.recordLog("AUTOMATION_TOGGLED", "AUTOMATION", saved.getId(), saved.getName(), null, null, null,
                "Automation rule is now " + (saved.getIsEnabled() ? "ENABLED" : "DISABLED"));
        return toRuleDTO(saved);
    }

    private AutomationRuleDTO toRuleDTO(AutomationRule r) {
        return AutomationRuleDTO.builder()
                .id(r.getId())
                .projectId(r.getProjectId())
                .name(r.getName())
                .description(r.getDescription())
                .triggerEvent(r.getTriggerEvent().name())
                .actionType(r.getActionType().name())
                .targetRole(r.getTargetRole())
                .isEnabled(r.getIsEnabled())
                .executionCount(r.getExecutionCount())
                .lastExecutedAt(r.getLastExecutedAt())
                .build();
    }

    // ---------------------------------------------------------------- financials

    /**
     * Cost = logged hours × hourly rate of the logging user's role (rates from
     * {@code syncflow.finance.role-hourly-rates}). Projection adds the remaining estimated hours of
     * open tasks at the project's blended rate.
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectFinancialsDTO getProjectFinancials(Long projectId) {
        Project project = requireProject(projectId);
        SyncFlowProperties.Finance finance = properties.getFinance();

        List<TaskTimeLog> logs = taskTimeLogRepository.findByTaskProjectIdOrderByCreatedAtDesc(projectId);
        Map<String, Double> spentByRole = new LinkedHashMap<>();
        Map<String, Double> ratesUsed = new LinkedHashMap<>();
        double spent = 0;
        double hours = 0;
        for (TaskTimeLog log : logs) {
            String role = log.getUser() != null && log.getUser().getRole() != null ? log.getUser().getRole().getName() : "UNASSIGNED";
            double rate = finance.getRoleHourlyRates().getOrDefault(role, finance.getDefaultHourlyRate());
            double cost = log.getHoursSpent() * rate;
            spentByRole.merge(role, cost, Double::sum);
            ratesUsed.putIfAbsent(role, rate);
            spent += cost;
            hours += log.getHoursSpent();
        }
        spentByRole.replaceAll((k, v) -> round2(v));

        List<Task> tasks = taskRepository.findByProjectIdAndIsArchivedFalse(projectId);
        int totalPoints = tasks.stream().mapToInt(this::points).sum();
        int completedPoints = tasks.stream().filter(TaskMapper::isDone).mapToInt(this::points).sum();
        double blendedRate = hours > 0 ? spent / hours : finance.getDefaultHourlyRate();
        double remainingHours = tasks.stream()
                .filter(t -> !TaskMapper.isDone(t))
                .mapToDouble(t -> Math.max(0, (t.getEstimatedHours() != null ? t.getEstimatedHours() : 0)
                        - (t.getTotalHoursSpent() != null ? t.getTotalHoursSpent() : 0)))
                .sum();

        Double totalBudget = project.getTotalBudget();
        return ProjectFinancialsDTO.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .currency(project.getCurrency() != null ? project.getCurrency() : "USD")
                .totalBudget(totalBudget)
                .allocatedBudget(project.getAllocatedBudget())
                .spentBudget(round2(spent))
                .remainingBudget(totalBudget != null ? round2(Math.max(0, totalBudget - spent)) : null)
                .burnRatePercent(totalBudget != null && totalBudget > 0 ? round1(100.0 * spent / totalBudget) : null)
                .estimatedCostPerStoryPoint(completedPoints > 0 ? round2(spent / completedPoints) : null)
                .totalStoryPoints(totalPoints)
                .completedStoryPoints(completedPoints)
                .roleHourlyRates(ratesUsed.isEmpty() ? new LinkedHashMap<>(finance.getRoleHourlyRates()) : ratesUsed)
                .spentByRole(spentByRole)
                .projectedCostAtCompletion(round2(spent + remainingHours * blendedRate))
                .totalHoursLogged(round1(hours))
                .blendedHourlyRate(round2(blendedRate))
                .build();
    }

    // ---------------------------------------------------------------- capacity

    /**
     * Load per member = story points of their open (not done, not archived) tasks, scoped to the
     * active sprint when one exists. Members are the project's team (or all active users for
     * organisation-wide projects).
     */
    @Override
    @Transactional(readOnly = true)
    public TeamCapacityDTO getTeamCapacity(Long projectId) {
        Project project = requireProject(projectId);
        int maxCap = properties.getCapacity().getDefaultStoryPointsPerMember();
        Optional<Sprint> activeSprint = sprintRepository.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE);

        List<Task> openTasks = taskRepository.findByProjectIdAndIsArchivedFalse(projectId).stream()
                .filter(t -> !TaskMapper.isDone(t))
                .filter(t -> activeSprint.isEmpty() || activeSprint.get().getId().equals(t.getSprintId()))
                .toList();
        Map<UUID, List<Task>> byAssignee = openTasks.stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getId()));

        List<User> members = projectRepositoryPort.findTeamMembers(projectId).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .toList();

        List<MemberCapacityDTO> memberDTOs = new ArrayList<>();
        int totalCapacity = 0;
        int totalAssigned = 0;
        int overloaded = 0;
        for (User u : members) {
            List<Task> memberTasks = byAssignee.getOrDefault(u.getId(), List.of());
            int assigned = memberTasks.stream().mapToInt(this::points).sum();
            boolean onLeave = Boolean.TRUE.equals(u.getOnLeave());
            int capacity = onLeave ? 0 : maxCap;
            boolean isOverloaded = assigned > capacity;

            String status;
            if (onLeave) status = "ON_LEAVE";
            else if (isOverloaded) status = "OVERLOADED";
            else if (assigned < capacity * 0.5) status = "UNDERUTILIZED";
            else status = "OPTIMAL";
            if (isOverloaded && !onLeave) overloaded++;

            totalCapacity += capacity;
            totalAssigned += assigned;
            memberDTOs.add(MemberCapacityDTO.builder()
                    .userId(u.getId().toString())
                    .fullName(u.getFullName())
                    .role(u.getRole() != null ? u.getRole().getName() : "Team Member")
                    .avatar(TaskMapper.initials(u.getFullName()))
                    .maxCapacityStoryPoints(capacity)
                    .assignedStoryPoints(assigned)
                    .utilizationPercent(capacity > 0 ? round1(100.0 * assigned / capacity) : (assigned > 0 ? 100.0 : 0.0))
                    .assignedTaskCount(memberTasks.size())
                    .isOverloaded(isOverloaded)
                    .onLeave(onLeave)
                    .status(status)
                    .assignedTaskTitles(memberTasks.stream().map(Task::getTitle).toList())
                    .build());
        }

        return TeamCapacityDTO.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .totalSprintCapacity(totalCapacity)
                .totalAssignedPoints(totalAssigned)
                .overallUtilizationPercent(totalCapacity > 0 ? round1(100.0 * totalAssigned / totalCapacity) : 0.0)
                .overloadedMembersCount(overloaded)
                .members(memberDTOs)
                .build();
    }

    // ---------------------------------------------------------------- webhooks

    @Override
    @Transactional(readOnly = true)
    public List<WebhookSubscriptionDTO> getWebhooks(Long projectId) {
        return webhookSubscriptionRepository.findByProjectId(projectId).stream()
                .map(WebhookMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public WebhookSubscriptionDTO createWebhook(Long projectId, CreateWebhookRequest request) {
        requireProject(projectId);
        webhookDelivery.validateTarget(request.getTargetUrl());

        Set<String> events = new LinkedHashSet<>();
        if (request.getEvents() == null || request.getEvents().isEmpty()) {
            events.addAll(List.of(ProjectEvent.TASK_CREATED, ProjectEvent.TASK_MOVED, ProjectEvent.TASK_BLOCKED,
                    ProjectEvent.SPRINT_COMPLETED));
        } else {
            for (String e : request.getEvents()) {
                String event = e.trim().toUpperCase(Locale.ROOT);
                if (!ProjectEvent.SUPPORTED.contains(event)) {
                    throw new DomainException("Unsupported webhook event: " + e + ". Supported: " + ProjectEvent.SUPPORTED);
                }
                events.add(event);
            }
        }

        WebhookSubscription saved = webhookSubscriptionRepository.save(WebhookSubscription.builder()
                .projectId(projectId)
                .name(request.getName().trim())
                .targetUrl(request.getTargetUrl().trim())
                .secretToken(request.getSecretToken() != null && !request.getSecretToken().isBlank()
                        ? request.getSecretToken().trim() : null)
                .events(String.join(",", events))
                .isEnabled(true)
                .deliveryCount(0)
                .build());
        auditLogUseCase.recordLog("WEBHOOK_CREATED", "PROJECT", projectId, saved.getName(), null, null, null,
                "Registered webhook to " + saved.getTargetUrl() + " for " + saved.getEvents());
        return WebhookMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public WebhookSubscriptionDTO toggleWebhook(Long webhookId) {
        WebhookSubscription sub = webhookSubscriptionRepository.findById(webhookId)
                .orElseThrow(() -> new NotFoundException("Webhook not found: " + webhookId));
        sub.setIsEnabled(!Boolean.TRUE.equals(sub.getIsEnabled()));
        return WebhookMapper.toDTO(webhookSubscriptionRepository.save(sub));
    }

    /** Not transactional: performs a live HTTP call that must not hold a database connection. */
    @Override
    public WebhookSubscriptionDTO testWebhook(Long webhookId) {
        return WebhookMapper.toDTO(webhookDelivery.sendTestPing(webhookId));
    }

    @Override
    @Transactional
    public void deleteWebhook(Long webhookId) {
        WebhookSubscription sub = webhookSubscriptionRepository.findById(webhookId)
                .orElseThrow(() -> new NotFoundException("Webhook not found: " + webhookId));
        webhookSubscriptionRepository.delete(sub);
        auditLogUseCase.recordLog("WEBHOOK_DELETED", "PROJECT", sub.getProjectId(), sub.getName(), null, null, null,
                "Deleted webhook to " + sub.getTargetUrl());
    }

    // ---------------------------------------------------------------- time summary

    @Override
    @Transactional(readOnly = true)
    public ProjectTimeSummaryDTO getProjectTimeSummary(Long projectId) {
        Project project = requireProject(projectId);
        List<Task> tasks = taskRepository.findByProjectIdAndIsArchivedFalse(projectId);
        List<TaskTimeLog> allLogs = taskTimeLogRepository.findByTaskProjectIdOrderByCreatedAtDesc(projectId);

        double totalHoursSpent = allLogs.stream().mapToDouble(TaskTimeLog::getHoursSpent).sum();
        double totalEstimatedHours = tasks.stream()
                .mapToDouble(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : 0.0)
                .sum();
        double hoursRemaining = Math.max(0.0, totalEstimatedHours - totalHoursSpent);
        double progressPercentage = totalEstimatedHours > 0
                ? Math.min(100.0, round1(100.0 * totalHoursSpent / totalEstimatedHours))
                : 0.0;

        Map<UUID, List<TaskTimeLog>> logsByUser = allLogs.stream()
                .filter(l -> l.getUser() != null)
                .collect(Collectors.groupingBy(l -> l.getUser().getId()));

        List<DeveloperTimeBreakdownDTO> devBreakdowns = new ArrayList<>();
        for (List<TaskTimeLog> userLogs : logsByUser.values()) {
            User u = userLogs.get(0).getUser();
            double userHours = userLogs.stream().mapToDouble(TaskTimeLog::getHoursSpent).sum();
            devBreakdowns.add(DeveloperTimeBreakdownDTO.builder()
                    .userId(u.getId())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .role(u.getRole() != null ? u.getRole().getName() : "CONTRIBUTOR")
                    .totalHours(round1(userHours))
                    .logsCount(userLogs.size())
                    .percentageOfTotal(totalHoursSpent > 0 ? round1(100.0 * userHours / totalHoursSpent) : 0.0)
                    .build());
        }
        devBreakdowns.sort((a, b) -> Double.compare(b.getTotalHours(), a.getTotalHours()));

        Map<Long, List<Task>> tasksByStage = tasks.stream()
                .filter(t -> t.getStage() != null)
                .collect(Collectors.groupingBy(t -> t.getStage().getId()));
        List<StageTimeBreakdownDTO> stageBreakdowns = new ArrayList<>();
        for (Stage s : stageRepositoryPort.findByProjectIdOrderByPositionAsc(projectId)) {
            List<Task> stageTasks = tasksByStage.getOrDefault(s.getId(), List.of());
            double stageHours = stageTasks.stream()
                    .mapToDouble(t -> t.getTotalHoursSpent() != null ? t.getTotalHoursSpent() : 0.0)
                    .sum();
            stageBreakdowns.add(StageTimeBreakdownDTO.builder()
                    .stageId(s.getId())
                    .stageName(s.getName())
                    .totalHours(round1(stageHours))
                    .tasksCount(stageTasks.size())
                    .percentageOfTotal(totalHoursSpent > 0 ? round1(100.0 * stageHours / totalHoursSpent) : 0.0)
                    .build());
        }

        List<TaskTimeLogDTO> recentLogs = allLogs.stream().limit(10).map(l -> TaskTimeLogDTO.builder()
                .id(l.getId())
                .taskId(l.getTask() != null ? l.getTask().getId() : null)
                .taskTitle(l.getTask() != null ? l.getTask().getTitle() : null)
                .userId(l.getUser() != null ? l.getUser().getId() : null)
                .userName(l.getUser() != null ? l.getUser().getFullName() : "Unknown")
                .userEmail(l.getUser() != null ? l.getUser().getEmail() : "")
                .userRole(l.getUser() != null && l.getUser().getRole() != null ? l.getUser().getRole().getName() : "CONTRIBUTOR")
                .hoursSpent(l.getHoursSpent())
                .description(l.getDescription())
                .loggedDate(l.getLoggedDate())
                .createdAt(l.getCreatedAt())
                .build()).toList();

        return ProjectTimeSummaryDTO.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .totalHoursSpent(round1(totalHoursSpent))
                .totalEstimatedHours(round1(totalEstimatedHours))
                .hoursRemaining(round1(hoursRemaining))
                .progressPercentage(progressPercentage)
                .totalLogsCount(allLogs.size())
                .tasksWithLogsCount((int) tasks.stream().filter(t -> t.getTotalHoursSpent() != null && t.getTotalHoursSpent() > 0).count())
                .developerBreakdown(devBreakdowns)
                .stageBreakdown(stageBreakdowns)
                .recentLogs(recentLogs)
                .build();
    }

    // ---------------------------------------------------------------- helpers

    private Project requireProject(Long projectId) {
        return projectRepositoryPort.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private Sprint requireSprint(Long sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found: " + sprintId));
    }

    private int points(Task t) {
        return t.getStoryPoints() != null ? t.getStoryPoints() : 3;
    }

    private ProjectDTO toProjectDTO(Project p) {
        Optional<UserDetailsImpl> me = currentUser.get();
        ProjectRole myRole = me.flatMap(u -> projectRepositoryPort.findMember(p.getId(), u.getId()))
                .map(ProjectMember::getRole).orElse(null);
        boolean admin = me.map(CurrentUser::isAdmin).orElse(false);
        return ProjectDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .status(p.getStatus().name())
                .startDate(p.getStartDate())
                .dueDate(p.getDueDate())
                .managerName(p.getManager() != null ? p.getManager().getFullName() : null)
                .memberCount(projectRepositoryPort.countMembers(p.getId()))
                .myRole(myRole != null ? myRole.name() : null)
                .canManageTeam(admin || (myRole != null && myRole.canManageTeam()))
                .build();
    }

    private StageDTO toStageDTO(Stage s, List<TaskDTO> tasks) {
        return StageDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .position(s.getPosition())
                .isDoneStage(s.getIsDoneStage())
                .requiresChecklistComplete(s.getRequiresChecklistComplete())
                .requiresDeliverable(s.getRequiresDeliverable())
                .requiresNoBlockers(s.getRequiresNoBlockers())
                .tasks(tasks)
                .build();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
