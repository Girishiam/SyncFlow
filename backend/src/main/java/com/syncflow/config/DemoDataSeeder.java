package com.syncflow.config;

import com.syncflow.model.enums.*;

import com.syncflow.model.*;
import com.syncflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Loads demo users, projects, tasks and chat history for local development ('dev' profile only).
 * Permissions and system roles are created by {@link ReferenceDataInitializer}.
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final StageRepository stageRepository;
    private final TaskRepository taskRepository;
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final MessageSeenReceiptRepository receiptRepository;
    private final com.syncflow.notification.infrastructure.persistence.SpringDataNotificationRepository notificationRepository;
    private final com.syncflow.audit.infrastructure.persistence.SpringDataAuditLogRepository auditLogRepository;
    private final com.syncflow.audit.application.port.in.AuditLogUseCase auditLogUseCase;
    private final com.syncflow.task.infrastructure.persistence.SpringDataSubtaskRepository subtaskRepository;
    private final com.syncflow.task.infrastructure.persistence.SpringDataTaskAttachmentRepository attachmentRepository;
    private final SprintRepository sprintRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final AutomationRuleRepository automationRuleRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final com.syncflow.repository.TaskTimeLogRepository taskTimeLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding SyncFlow demo data (dev profile)...");

        Role rAdmin = role("ADMIN");
        Role rManager = role("REPORTING_MANAGER");
        Role rTeamLead = role("TEAM_LEAD");
        Role rDev = role("DEVELOPER");
        Role rSqa = role("SQA");

        // 3. Create Seed Users
        User admin = createUserIfNotFound("admin@syncflow.bank", "Admin@123", "Chief Admin", rAdmin);
        User manager = createUserIfNotFound("manager@syncflow.bank", "Manager@123", "Tariq Manager", rManager);
        User lead = createUserIfNotFound("lead@syncflow.bank", "Lead@123", "Arman Lead", rTeamLead);
        User dev = createUserIfNotFound("dev@syncflow.bank", "Dev@123", "Farhan Senior Dev", rDev);
        User sqa = createUserIfNotFound("sqa@syncflow.bank", "Sqa@123", "Sarah SQA Engineer", rSqa);

        // 4. Create Projects if none exist
        if (projectRepository.count() == 0) {
            Project p1 = projectRepository.save(Project.builder()
                    .name("Core Payment Gateway v2.4")
                    .description("Core banking high-throughput transaction routing and settlement")
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.now().minusDays(14))
                    .dueDate(LocalDate.now().plusDays(45))
                    .manager(manager)
                    .build());

            projectRepository.save(Project.builder()
                    .name("Biometric 2FA Engine")
                    .description("FIDO2 and WebAuthn hardware key verification service")
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.now().minusDays(5))
                    .dueDate(LocalDate.now().plusDays(30))
                    .manager(manager)
                    .build());

            projectRepository.save(Project.builder()
                    .name("Liquidity Settlement")
                    .description("Multi-currency Nostro/Vostro real-time liquidity balancing")
                    .status(ProjectStatus.ACTIVE)
                    .startDate(LocalDate.now().minusDays(20))
                    .dueDate(LocalDate.now().plusDays(60))
                    .manager(manager)
                    .build());

            // 5. Create Pipeline Stages for Core Payment Gateway
            Stage sBacklog = stageRepository.save(Stage.builder().project(p1).name("Backlog").position(0).isDoneStage(false).build());
            Stage sDb = stageRepository.save(Stage.builder().project(p1).name("DB Design").position(1).isDoneStage(false).build());
            Stage sApi = stageRepository.save(Stage.builder().project(p1).name("API Dev").position(2).isDoneStage(false).build());
            Stage sUi = stageRepository.save(Stage.builder().project(p1).name("UI & Layout").position(3).isDoneStage(false).build());
            Stage sReview = stageRepository.save(Stage.builder().project(p1).name("Review / QA").position(4).isDoneStage(false).requiresChecklistComplete(true).build());
            Stage sDone = stageRepository.save(Stage.builder().project(p1).name("Done").position(5).isDoneStage(true).requiresDeliverable(true).requiresNoBlockers(true).build());

            // 6. Create Tasks for Core Payment Gateway
            taskRepository.save(Task.builder()
                    .project(p1).stage(sBacklog).title("Implement Biometric 2FA Hook")
                    .description("Integrate FIDO2 challenge-response verification for transactions")
                    .priority(TaskPriority.HIGH).progressPercentage(0).assignee(dev).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sBacklog).title("Audit Trail Export to CSV")
                    .description("Generate encrypted compliance audit logs for central bank audit")
                    .priority(TaskPriority.LOW).progressPercentage(0).assignee(sqa).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sDb).title("MSSQL Partitioning for Ledger")
                    .description("Implement year-based table partitioning on transactions table")
                    .priority(TaskPriority.CRITICAL).progressPercentage(30).assignee(dev).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sApi).title("WebSocket STOMP Broker & Event Hook")
                    .description("Enable live drag-and-drop and real-time blocker synchronization")
                    .priority(TaskPriority.HIGH).progressPercentage(65).assignee(lead)
                    .isBlocked(true).blockerReason("Waiting for SSL certificate handshake setup").build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sUi).title("Emerald Reserve Design Tokens")
                    .description("Refactor CSS tokens for bank primary #006022 and gold accents")
                    .priority(TaskPriority.CRITICAL).progressPercentage(90).assignee(dev).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sReview).title("JWT Refresh Filter Test Suite")
                    .description("Execute integration test suite covering expired and revoked tokens")
                    .priority(TaskPriority.MEDIUM).progressPercentage(95).assignee(sqa).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sDone).title("Spring Boot 3.3.5 & Java 21 Loom Scaffolding")
                    .description("Scaffold core backend architecture with virtual threads enabled")
                    .priority(TaskPriority.HIGH).progressPercentage(100).assignee(dev).isBlocked(false)
                    .completedAt(java.time.Instant.now().minusSeconds(86400)).build());

            log.info("Created Seed Project, Stages, and Tasks!");
        }

        // 7. Create Channels if none exist
        if (channelRepository.count() == 0) {
            Channel chGeneral = channelRepository.save(Channel.builder().name("general").type(ChannelType.TEXT).createdBy(admin).build());
            Channel chApi = channelRepository.save(Channel.builder().name("core-banking-api").type(ChannelType.TEXT).createdBy(lead).build());
            channelRepository.save(Channel.builder().name("design-tokens").type(ChannelType.TEXT).createdBy(dev).build());
            channelRepository.save(Channel.builder().name("database-migration").type(ChannelType.TEXT).createdBy(lead).build());

            // Add sample messages to #general
            Message m1 = messageRepository.save(Message.builder()
                    .channel(chGeneral).sender(manager)
                    .content("Sprint goals are locked in for the payment gateway migration.").build());

            Message m2 = messageRepository.save(Message.builder()
                    .channel(chGeneral).sender(lead)
                    .content("Pipeline stages are configured. API development is underway.").build());

            Message m3 = messageRepository.save(Message.builder()
                    .channel(chGeneral).sender(dev)
                    .content("MSSQL connection pool and Virtual Threads setup complete.").build());

            // Add Seen receipts
            receiptRepository.save(MessageSeenReceipt.builder().message(m1).channel(chGeneral).user(lead).build());
            receiptRepository.save(MessageSeenReceipt.builder().message(m1).channel(chGeneral).user(dev).build());
            receiptRepository.save(MessageSeenReceipt.builder().message(m2).channel(chGeneral).user(dev).build());
            receiptRepository.save(MessageSeenReceipt.builder().message(m3).channel(chGeneral).user(lead).build());

            log.info("Created Seed Channels and Messages!");
        }

        // 8. Seed Initial Notifications for Bank Roles
        if (notificationRepository.count() == 0) {
            java.time.Instant now = java.time.Instant.now();

            // Task Assignment Notification for Dev
            notificationRepository.save(Notification.builder()
                    .recipient(dev)
                    .title("New Task Assigned: MSSQL Partitioning for Ledger")
                    .message("Tariq Manager assigned you to 'MSSQL Partitioning for Ledger' (CRITICAL priority). Due: Tomorrow at 05:00 PM.")
                    .type(NotificationType.TASK_ASSIGNED)
                    .priority(NotificationPriority.CRITICAL)
                    .isRead(false)
                    .actionType(NotificationAction.NAVIGATE_TASK)
                    .referenceId(3L)
                    .senderName("Tariq Manager")
                    .senderAvatar("TM")
                    .createdAt(now.minusSeconds(3600))
                    .build());

            // Task Due Soon Notification for Dev
            notificationRepository.save(Notification.builder()
                    .recipient(dev)
                    .title("Upcoming Deadline: Emerald Reserve Design Tokens")
                    .message("Reminder: Task 'Emerald Reserve Design Tokens' is due in less than 24 hours. Stage: UI & Layout.")
                    .type(NotificationType.TASK_DUE_SOON)
                    .priority(NotificationPriority.WARNING)
                    .isRead(false)
                    .actionType(NotificationAction.NAVIGATE_TASK)
                    .referenceId(5L)
                    .senderName("SyncFlow Automated System")
                    .senderAvatar("SF")
                    .createdAt(now.minusSeconds(1800))
                    .build());

            // Channel Mention for Dev
            notificationRepository.save(Notification.builder()
                    .recipient(dev)
                    .title("Mentioned in #core-banking-api")
                    .message("Arman Lead mentioned you in #core-banking-api: \"@dev please verify if the connection pool supports 100 concurrent requests.\"")
                    .type(NotificationType.CHANNEL_MENTION)
                    .priority(NotificationPriority.INFO)
                    .isRead(false)
                    .actionType(NotificationAction.NAVIGATE_CHANNEL)
                    .referenceId(2L)
                    .senderName("Arman Lead")
                    .senderAvatar("AL")
                    .createdAt(now.minusSeconds(900))
                    .build());

            // Overdue Task for Lead
            notificationRepository.save(Notification.builder()
                    .recipient(lead)
                    .title("OVERDUE: WebSocket STOMP Broker & Event Hook")
                    .message("CRITICAL: Task 'WebSocket STOMP Broker & Event Hook' deadline has passed. Delivery update required.")
                    .type(NotificationType.TASK_OVERDUE)
                    .priority(NotificationPriority.CRITICAL)
                    .isRead(false)
                    .actionType(NotificationAction.NAVIGATE_TASK)
                    .referenceId(4L)
                    .senderName("SyncFlow Automated System")
                    .senderAvatar("SF")
                    .createdAt(now.minusSeconds(7200))
                    .build());

            // Channel Mention for Lead
            notificationRepository.save(Notification.builder()
                    .recipient(lead)
                    .title("Mentioned in #general")
                    .message("Tariq Manager mentioned you in #general: \"@lead pipeline review is scheduled for today at 3 PM.\"")
                    .type(NotificationType.CHANNEL_MENTION)
                    .priority(NotificationPriority.INFO)
                    .isRead(false)
                    .actionType(NotificationAction.NAVIGATE_CHANNEL)
                    .referenceId(1L)
                    .senderName("Tariq Manager")
                    .senderAvatar("TM")
                    .createdAt(now.minusSeconds(2400))
                    .build());

            // Blocker alert for the leadership roles (one row per recipient)
            for (User leader : List.of(admin, manager, lead)) {
                notificationRepository.save(Notification.builder()
                        .recipient(leader)
                        .title("Sprint Blocker Alert: WebSocket STOMP Broker & Event Hook")
                        .message("Arman Lead reported a blocker on 'WebSocket STOMP Broker & Event Hook': Waiting for SSL certificate handshake setup.")
                        .type(NotificationType.BLOCKER_ALERT)
                        .priority(NotificationPriority.CRITICAL)
                        .isRead(false)
                        .actionType(NotificationAction.NAVIGATE_TASK)
                        .referenceId(4L)
                        .senderName("Arman Lead")
                        .senderAvatar("AL")
                        .createdAt(now.minusSeconds(1200))
                        .build());
            }

            // Assignment for SQA
            notificationRepository.save(Notification.builder()
                    .recipient(sqa)
                    .title("New Task Assigned: JWT Refresh Filter Test Suite")
                    .message("Arman Lead assigned you to 'JWT Refresh Filter Test Suite' (MEDIUM priority).")
                    .type(NotificationType.TASK_ASSIGNED)
                    .priority(NotificationPriority.INFO)
                    .isRead(false)
                    .actionType(NotificationAction.NAVIGATE_TASK)
                    .referenceId(6L)
                    .senderName("Arman Lead")
                    .senderAvatar("AL")
                    .createdAt(now.minusSeconds(5000))
                    .build());

            log.info("Seeded Initial Bank Role Notifications!");
        }

        // 9. Seed Initial Subtasks & Deliverable QA Checklists
        if (subtaskRepository.count() == 0) {
            List<Task> allTasks = taskRepository.findAll();
            if (!allTasks.isEmpty()) {
                Task t1 = allTasks.get(0); // Biometric 2FA
                subtaskRepository.save(Subtask.builder().task(t1).title("FIDO2 Challenge-response payload schema").isCompleted(true).completedBy("Farhan Senior Dev").position(1).build());
                subtaskRepository.save(Subtask.builder().task(t1).title("WebAuthn navigator.credentials hook").isCompleted(true).completedBy("Farhan Senior Dev").position(2).build());
                subtaskRepository.save(Subtask.builder().task(t1).title("HSM key store integration & testing").isCompleted(false).position(3).build());

                if (allTasks.size() > 1) {
                    Task t2 = allTasks.get(1); // Audit Trail Export to CSV
                    subtaskRepository.save(Subtask.builder().task(t2).title("Define RFC 4180 CSV compliant headers").isCompleted(true).completedBy("Sarah SQA Engineer").position(1).build());
                    subtaskRepository.save(Subtask.builder().task(t2).title("Generate SHA-256 tamper-evident verification hash").isCompleted(true).completedBy("Sarah SQA Engineer").position(2).build());
                    subtaskRepository.save(Subtask.builder().task(t2).title("Streaming download response filter").isCompleted(true).completedBy("Sarah SQA Engineer").position(3).build());
                    subtaskRepository.save(Subtask.builder().task(t2).title("Central Bank compliance QA sign-off").isCompleted(true).completedBy("Sarah SQA Engineer").position(4).build());
                }

                if (allTasks.size() > 2) {
                    Task t3 = allTasks.get(2); // MSSQL Partitioning
                    subtaskRepository.save(Subtask.builder().task(t3).title("Analyze partition function on transaction_date").isCompleted(true).completedBy("Farhan Senior Dev").position(1).build());
                    subtaskRepository.save(Subtask.builder().task(t3).title("Define partition schemes across PRIMARY filegroups").isCompleted(false).position(2).build());
                    subtaskRepository.save(Subtask.builder().task(t3).title("Benchmark query execution latency under 100k rows").isCompleted(false).position(3).build());
                }
            }
            log.info("Seeded Initial Subtasks & QA Checklists!");
        }

        // 10. Seed Initial Task File & Screenshot Attachments
        if (attachmentRepository.count() == 0) {
            List<Task> allTasks = taskRepository.findAll();
            if (allTasks.size() > 1) {
                Task t2 = allTasks.get(1); // Audit Trail Export
                attachmentRepository.save(TaskAttachment.builder()
                        .task(t2)
                        .fileName("central_bank_compliance_spec_v2.pdf")
                        .fileType("link")
                        .fileUrl("https://example.com/specs/central_bank_compliance_spec_v2.pdf")
                        .uploadedBy("Sarah SQA Engineer")
                        .build());
                attachmentRepository.save(TaskAttachment.builder()
                        .task(t2)
                        .fileName("audit_export_sample_2026.csv")
                        .fileType("link")
                        .fileUrl("https://example.com/samples/audit_export_sample_2026.csv")
                        .uploadedBy("Sarah SQA Engineer")
                        .build());
            }
            if (allTasks.size() > 0) {
                Task t1 = allTasks.get(0);
                attachmentRepository.save(TaskAttachment.builder()
                        .task(t1)
                        .fileName("fido2_biometric_flowchart.png")
                        .fileType("link")
                        .fileUrl("https://example.com/diagrams/fido2_biometric_flowchart.png")
                        .uploadedBy("Farhan Senior Dev")
                        .build());
            }
            log.info("Seeded Initial Task Attachments!");
        }

        // 11. Seed a few ledger entries through the real (hash-chained) audit service
        if (auditLogRepository.count() == 0) {
            auditLogUseCase.recordLog("TASK_CREATED", "TASK", 2L, "Audit Trail Export to CSV",
                    "Tariq Manager", "manager@syncflow.bank", "REPORTING_MANAGER",
                    "Created task with LOW priority under project Core Payment Gateway v2.4");
            auditLogUseCase.recordLog("TASK_ASSIGNED", "TASK", 2L, "Audit Trail Export to CSV",
                    "Tariq Manager", "manager@syncflow.bank", "REPORTING_MANAGER",
                    "Assigned deliverable to Sarah SQA Engineer");
            auditLogUseCase.recordLog("TASK_BLOCKED", "TASK", 4L, "WebSocket STOMP Broker & Event Hook",
                    "Arman Lead", "lead@syncflow.bank", "TEAM_LEAD",
                    "Active blocker logged: Waiting for SSL certificate handshake setup");
            log.info("Seeded demo audit ledger entries");
        }

        // 10. Seed Agile Sprints, Story Points, Dependencies & Butler Automations
        if (sprintRepository.count() == 0 && projectRepository.count() > 0) {
            Project p1 = projectRepository.findAll().get(0);
            LocalDate today = LocalDate.now();

            Sprint s42 = sprintRepository.save(Sprint.builder()
                    .projectId(p1.getId())
                    .name("Sprint 42 - Payment Core")
                    .goal("Core Payment Gateway v2.4 hardening & multi-currency settlement")
                    .startDate(today.minusDays(5))
                    .endDate(today.plusDays(9))
                    .status(SprintStatus.ACTIVE)
                    .targetStoryPoints(34)
                    .createdAt(java.time.Instant.now().minusSeconds(86400 * 5))
                    .build());

            Sprint s43 = sprintRepository.save(Sprint.builder()
                    .projectId(p1.getId())
                    .name("Sprint 43 - Biometric 2FA & FIDO2")
                    .goal("Biometric hardware keys and audit trail compliance")
                    .startDate(today.plusDays(10))
                    .endDate(today.plusDays(24))
                    .status(SprintStatus.PLANNING)
                    .targetStoryPoints(40)
                    .createdAt(java.time.Instant.now())
                    .build());

            // Assign story points and sprint to existing tasks
            List<Task> tasks = taskRepository.findByProjectId(p1.getId());
            int[] pointsArr = {5, 3, 8, 5, 3, 5, 5};
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                t.setSprintId(s42.getId());
                t.setStoryPoints(i < pointsArr.length ? pointsArr[i] : 3);
                t.setIsArchived(false);
                if (t.getStartDate() == null) {
                    t.setStartDate(java.time.Instant.now().minusSeconds(86400 * (4 - Math.min(i, 4))));
                }
                taskRepository.save(t);
            }

            // Seed Task Dependencies if any
            if (tasks.size() >= 4) {
                Task t1 = tasks.get(0);
                Task t3 = tasks.get(2);
                Task t4 = tasks.get(3);

                taskDependencyRepository.save(TaskDependency.builder()
                        .sourceTaskId(t1.getId())
                        .targetTaskId(t3.getId())
                        .targetTaskTitle(t3.getTitle())
                        .dependencyType(DependencyType.BLOCKS)
                        .createdBy("Arman Lead")
                        .createdAt(java.time.Instant.now())
                        .build());

                taskDependencyRepository.save(TaskDependency.builder()
                        .sourceTaskId(t4.getId())
                        .targetTaskId(t1.getId())
                        .targetTaskTitle(t1.getTitle())
                        .dependencyType(DependencyType.BLOCKED_BY)
                        .createdBy("Farhan Senior Dev")
                        .createdAt(java.time.Instant.now())
                        .build());
            }

            // Seed 4 Butler Automation Rules
            automationRuleRepository.save(AutomationRule.builder()
                    .projectId(p1.getId())
                    .name("Auto-Mark Done on Checklist Completion")
                    .description("When all subtasks in a task checklist are completed, automatically move card to Done stage.")
                    .triggerEvent(AutomationTrigger.ALL_SUBTASKS_COMPLETE)
                    .actionType(AutomationAction.MOVE_TO_DONE)
                    .isEnabled(true)
                    .executionCount(0)
                    .createdAt(java.time.Instant.now())
                    .build());

            automationRuleRepository.save(AutomationRule.builder()
                    .projectId(p1.getId())
                    .name("Auto-Assign QA Lead on Review Stage")
                    .description("When a card enters the Review / QA column, automatically assign Sarah SQA Engineer.")
                    .triggerEvent(AutomationTrigger.MOVED_TO_REVIEW)
                    .actionType(AutomationAction.ASSIGN_ROLE)
                    .targetRole("SQA")
                    .isEnabled(true)
                    .executionCount(0)
                    .createdAt(java.time.Instant.now())
                    .build());

            automationRuleRepository.save(AutomationRule.builder()
                    .projectId(p1.getId())
                    .name("Dependency Blocker Escalation to PM")
                    .description("When a task is marked blocked with critical priority, alert Project Manager immediately.")
                    .triggerEvent(AutomationTrigger.TASK_BLOCKED)
                    .actionType(AutomationAction.NOTIFY_MANAGER)
                    .targetRole("REPORTING_MANAGER")
                    .isEnabled(true)
                    .executionCount(0)
                    .createdAt(java.time.Instant.now())
                    .build());

            automationRuleRepository.save(AutomationRule.builder()
                    .projectId(p1.getId())
                    .name("Sprint Closure Auto-Archive")
                    .description("When an active sprint is marked completed, automatically archive all finished tasks.")
                    .triggerEvent(AutomationTrigger.SPRINT_CLOSED)
                    .actionType(AutomationAction.ARCHIVE_DONE_TASKS)
                    .isEnabled(false)
                    .executionCount(0)
                    .createdAt(java.time.Instant.now())
                    .build());

            log.info("Seeded Initial Sprints, Task Dependencies, and Butler Automation Rules!");
        }

        // 11. Seed Initial Webhook Subscriptions & Bank Budgets
        if (webhookSubscriptionRepository.count() == 0) {
            Project p1 = projectRepository.findAll().stream().findFirst().orElse(null);
            if (p1 != null) {
                p1.setTotalBudget(250000.0);
                p1.setAllocatedBudget(195000.0);
                p1.setCurrency("USD");
                projectRepository.save(p1);

                webhookSubscriptionRepository.save(WebhookSubscription.builder()
                        .projectId(p1.getId())
                        .name("Slack #core-banking-ops Alerts")
                        .targetUrl("https://hooks.slack.com/services/T00/B00/syncflow-bank-ops")
                        .secretToken("whsec_bank_prod_98765")
                        .events("TASK_CREATED,TASK_MOVED,TASK_BLOCKED,SPRINT_COMPLETED")
                        .isEnabled(false) // demo endpoints are placeholders; enable after pointing at a real URL
                        .deliveryCount(0)
                        .build());

                webhookSubscriptionRepository.save(WebhookSubscription.builder()
                        .projectId(p1.getId())
                        .name("Jenkins CI/CD QA Pipeline Connector")
                        .targetUrl("https://ci.syncflow.internal/job/qa-smoke-tests/buildWithParameters")
                        .secretToken("whsec_jenkins_ci_54321")
                        .events("TASK_MOVED,TASK_CREATED")
                        .isEnabled(false) // demo endpoints are placeholders; enable after pointing at a real URL
                        .deliveryCount(0)
                        .build());

                // Seed sample Git commits / PRs on existing tasks
                List<Task> tasks = taskRepository.findByProjectId(p1.getId());
                if (!tasks.isEmpty()) {
                    Task t1 = tasks.get(0);
                    t1.setGitBranch("feature/CORE-101-oauth2-mfa");
                    t1.setGitCommitHash("c8f42d1");
                    t1.setGitPrUrl("https://github.com/syncflow-bank/core-auth/pull/412");
                    taskRepository.save(t1);

                    if (tasks.size() > 2) {
                        Task t3 = tasks.get(2);
                        t3.setGitBranch("refactor/CORE-204-mssql-partitioning");
                        t3.setGitCommitHash("9a1e7b4");
                        t3.setGitPrUrl("https://github.com/syncflow-bank/core-ledger/pull/189");
                        taskRepository.save(t3);
                    }
                }

                // Seed initial Time Tracking Logs
                if (taskTimeLogRepository.count() == 0 && !tasks.isEmpty()) {
                    Task t1 = tasks.get(0);
                    taskTimeLogRepository.save(com.syncflow.model.TaskTimeLog.builder()
                            .task(t1)
                            .user(dev)
                            .hoursSpent(4.5)
                            .description("Developed core token validation and database schemas")
                            .loggedDate(java.time.LocalDate.now().minusDays(2))
                            .createdAt(java.time.Instant.now().minusSeconds(172800))
                            .build());

                    taskTimeLogRepository.save(com.syncflow.model.TaskTimeLog.builder()
                            .task(t1)
                            .user(sqa)
                            .hoursSpent(2.0)
                            .description("Integration smoke test and boundary condition validation")
                            .loggedDate(java.time.LocalDate.now().minusDays(1))
                            .createdAt(java.time.Instant.now().minusSeconds(86400))
                            .build());

                    t1.setEstimatedHours(8.0);
                    t1.setTotalHoursSpent(6.5);
                    taskRepository.save(t1);

                    if (tasks.size() > 2) {
                        Task t3 = tasks.get(2);
                        taskTimeLogRepository.save(com.syncflow.model.TaskTimeLog.builder()
                                .task(t3)
                                .user(lead)
                                .hoursSpent(3.5)
                                .description("Partition boundary alignment and query execution plan tuning")
                                .loggedDate(java.time.LocalDate.now().minusDays(1))
                                .createdAt(java.time.Instant.now().minusSeconds(72000))
                                .build());

                        taskTimeLogRepository.save(com.syncflow.model.TaskTimeLog.builder()
                                .task(t3)
                                .user(dev)
                                .hoursSpent(5.0)
                                .description("Data migration scripts and index reorg")
                                .loggedDate(java.time.LocalDate.now())
                                .createdAt(java.time.Instant.now().minusSeconds(14400))
                                .build());

                        t3.setEstimatedHours(12.0);
                        t3.setTotalHoursSpent(8.5);
                        taskRepository.save(t3);
                    }
                }

                log.info("Seeded Initial Webhooks, Bank Budgets, Git Links, and Time Logs!");
            }
        }

        log.info("SyncFlow Bank Enterprise initialization complete!");
    }

    private Role role(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException("System role missing: " + name));
    }

    private User createUserIfNotFound(String email, String rawPassword, String fullName, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .status(UserStatus.ACTIVE)
                    .onLeave(false)
                    .role(role)
                    .build();
            User saved = userRepository.save(user);
            log.info("Created Seed User: {} with Role: {}", email, role.getName());
            return saved;
        });
    }
}
