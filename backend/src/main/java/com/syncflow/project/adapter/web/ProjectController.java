package com.syncflow.project.adapter.web;

import com.syncflow.dto.*;
import com.syncflow.project.application.port.in.ProjectUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private static final String VIEW = "@access.can('PROJECT:VIEW') and @access.project(#id)";
    private static final String EDIT = "@access.can('PROJECT:EDIT') and @access.project(#id)";

    private final ProjectUseCase projectUseCase;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        return ResponseEntity.ok(projectUseCase.getAllProjects());
    }

    @PostMapping
    @PreAuthorize("@access.can('PROJECT:CREATE')")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectUseCase.createProject(request));
    }

    @GetMapping("/{id}/board")
    @PreAuthorize(VIEW)
    public ResponseEntity<ProjectBoardDTO> getProjectBoard(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectBoard(id));
    }

    @PostMapping("/{id}/stages")
    @PreAuthorize("@access.can('STAGE:BUILD') and @access.project(#id)")
    public ResponseEntity<StageDTO> createStage(@PathVariable Long id, @Valid @RequestBody CreateStageRequest request) {
        return ResponseEntity.ok(projectUseCase.createStage(id, request));
    }

    /** Rename a stage or change its workflow gates. */
    @PutMapping("/stages/{stageId}")
    @PreAuthorize("@access.can('STAGE:BUILD') and @access.stage(#stageId)")
    public ResponseEntity<StageDTO> updateStage(@PathVariable Long stageId, @Valid @RequestBody UpdateStageRequest request) {
        return ResponseEntity.ok(projectUseCase.updateStage(stageId, request));
    }

    @PutMapping("/{id}/stages/reorder")
    @PreAuthorize("@access.can('STAGE:BUILD') and @access.project(#id)")
    public ResponseEntity<Void> reorderStages(@PathVariable Long id, @Valid @RequestBody ReorderStagesRequest request) {
        projectUseCase.reorderStages(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/analytics/burndown")
    @PreAuthorize(VIEW)
    public ResponseEntity<BurndownAnalyticsDTO> getBurndownAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getBurndownAnalytics(id));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<TimelineTaskDTO>> getProjectTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectTimeline(id));
    }

    // --- Sprints ---
    @GetMapping("/{id}/sprints")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<SprintDTO>> getProjectSprints(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectSprints(id));
    }

    @PostMapping("/{id}/sprints")
    @PreAuthorize("@access.can('SPRINT:MANAGE') and @access.project(#id)")
    public ResponseEntity<SprintDTO> createSprint(@PathVariable Long id, @RequestBody SprintDTO request) {
        return ResponseEntity.ok(projectUseCase.createSprint(id, request));
    }

    @PutMapping("/sprints/{sprintId}/start")
    @PreAuthorize("@access.can('SPRINT:MANAGE') and @access.sprint(#sprintId)")
    public ResponseEntity<SprintDTO> startSprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(projectUseCase.startSprint(sprintId));
    }

    @PutMapping("/sprints/{sprintId}/complete")
    @PreAuthorize("@access.can('SPRINT:MANAGE') and @access.sprint(#sprintId)")
    public ResponseEntity<SprintDTO> completeSprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(projectUseCase.completeSprint(sprintId));
    }

    // --- Automations ---
    @GetMapping("/{id}/automations")
    @PreAuthorize(VIEW)
    public ResponseEntity<List<AutomationRuleDTO>> getAutomationRules(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getAutomationRules(id));
    }

    @PutMapping("/automations/{ruleId}/toggle")
    @PreAuthorize("@access.can('PROJECT:EDIT') and @access.automation(#ruleId)")
    public ResponseEntity<AutomationRuleDTO> toggleAutomationRule(@PathVariable Long ruleId) {
        return ResponseEntity.ok(projectUseCase.toggleAutomationRule(ruleId));
    }

    // --- Financials & capacity ---
    @GetMapping("/{id}/financials")
    @PreAuthorize("@access.can('FINANCE:VIEW') and @access.project(#id)")
    public ResponseEntity<ProjectFinancialsDTO> getProjectFinancials(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectFinancials(id));
    }

    @GetMapping("/{id}/capacity")
    @PreAuthorize(VIEW)
    public ResponseEntity<TeamCapacityDTO> getTeamCapacity(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getTeamCapacity(id));
    }

    // --- Webhooks ---
    @GetMapping("/{id}/webhooks")
    @PreAuthorize(EDIT)
    public ResponseEntity<List<WebhookSubscriptionDTO>> getWebhooks(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getWebhooks(id));
    }

    @PostMapping("/{id}/webhooks")
    @PreAuthorize(EDIT)
    public ResponseEntity<WebhookSubscriptionDTO> createWebhook(@PathVariable Long id, @Valid @RequestBody CreateWebhookRequest request) {
        return ResponseEntity.ok(projectUseCase.createWebhook(id, request));
    }

    @PutMapping("/webhooks/{webhookId}/toggle")
    @PreAuthorize("@access.can('PROJECT:EDIT') and @access.webhook(#webhookId)")
    public ResponseEntity<WebhookSubscriptionDTO> toggleWebhook(@PathVariable Long webhookId) {
        return ResponseEntity.ok(projectUseCase.toggleWebhook(webhookId));
    }

    @PostMapping("/webhooks/{webhookId}/test")
    @PreAuthorize("@access.can('PROJECT:EDIT') and @access.webhook(#webhookId)")
    public ResponseEntity<WebhookSubscriptionDTO> testWebhook(@PathVariable Long webhookId) {
        return ResponseEntity.ok(projectUseCase.testWebhook(webhookId));
    }

    @DeleteMapping("/webhooks/{webhookId}")
    @PreAuthorize("@access.can('PROJECT:EDIT') and @access.webhook(#webhookId)")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long webhookId) {
        projectUseCase.deleteWebhook(webhookId);
        return ResponseEntity.noContent().build();
    }

    // --- Time tracking ---
    @GetMapping("/{id}/time-summary")
    @PreAuthorize(VIEW)
    public ResponseEntity<ProjectTimeSummaryDTO> getProjectTimeSummary(@PathVariable Long id) {
        return ResponseEntity.ok(projectUseCase.getProjectTimeSummary(id));
    }
}
