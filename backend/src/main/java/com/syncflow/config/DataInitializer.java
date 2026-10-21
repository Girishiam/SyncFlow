package com.syncflow.config;

import com.syncflow.model.*;
import com.syncflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing SyncFlow Bank Enterprise RBAC & Seed Data...");

        // 1. Create Core Granular Permissions
        Permission pCreateProject = getOrCreatePermission("PROJECT", "CREATE", "Create new enterprise project");
        Permission pViewProject = getOrCreatePermission("PROJECT", "VIEW", "View project details and pipeline");
        Permission pEditProject = getOrCreatePermission("PROJECT", "EDIT", "Edit project parameters");
        Permission pDeleteProject = getOrCreatePermission("PROJECT", "DELETE", "Delete project");

        Permission pCreateTask = getOrCreatePermission("TASK", "CREATE", "Create task");
        Permission pViewTask = getOrCreatePermission("TASK", "VIEW", "View task");
        Permission pEditTask = getOrCreatePermission("TASK", "EDIT", "Edit task and progress slider");
        Permission pDeleteTask = getOrCreatePermission("TASK", "DELETE", "Delete task");
        Permission pApproveTask = getOrCreatePermission("TASK", "APPROVE", "Approve completed task in review");
        Permission pAssignTask = getOrCreatePermission("TASK", "ASSIGN", "Assign tasks to developers");

        Permission pBuildStage = getOrCreatePermission("STAGE", "BUILD", "Build and reorder pipeline stages");
        Permission pHostMeeting = getOrCreatePermission("MEETING", "HOST", "Host instant department or squad meeting");
        Permission pCreateChannel = getOrCreatePermission("CHAT", "CREATE_CHANNEL", "Create enterprise discussion channels");
        Permission pEditChannel = getOrCreatePermission("CHAT", "EDIT_CHANNEL", "Edit channel name, description, and icon/logo");
        Permission pDeleteChannel = getOrCreatePermission("CHAT", "DELETE_CHANNEL", "Delete enterprise discussion channels");
        Permission pManageChannelMembers = getOrCreatePermission("CHAT", "MANAGE_MEMBERS", "Add or remove members from channels");

        Permission pManageUsers = getOrCreatePermission("ADMIN", "MANAGE_USERS", "Manage users and overrides");
        Permission pManageRoles = getOrCreatePermission("ADMIN", "MANAGE_ROLES", "Manage roles and permission matrix");

        // 2. Create Roles
        Role rAdmin = getOrCreateRole("ADMIN", "System Administrator with full master control", true);
        rAdmin.getPermissions().addAll(Set.of(
                pCreateProject, pViewProject, pEditProject, pDeleteProject,
                pCreateTask, pViewTask, pEditTask, pDeleteTask, pApproveTask, pAssignTask,
                pBuildStage, pHostMeeting, pCreateChannel, pEditChannel, pDeleteChannel, pManageChannelMembers, pManageUsers, pManageRoles
        ));
        roleRepository.save(rAdmin);

        Role rManager = getOrCreateRole("REPORTING_MANAGER", "Executive control, project lifecycle, and performance analytics", true);
        rManager.getPermissions().addAll(Set.of(
                pCreateProject, pViewProject, pEditProject,
                pCreateTask, pViewTask, pEditTask, pApproveTask, pAssignTask,
                pBuildStage, pHostMeeting, pCreateChannel, pEditChannel, pDeleteChannel, pManageChannelMembers
        ));
        roleRepository.save(rManager);

        Role rTeamLead = getOrCreateRole("TEAM_LEAD", "Squad leader, pipeline stage builder, review queue, and task assignment", true);
        rTeamLead.getPermissions().addAll(Set.of(
                pViewProject, pEditProject,
                pCreateTask, pViewTask, pEditTask, pApproveTask, pAssignTask,
                pBuildStage, pHostMeeting, pCreateChannel, pEditChannel, pManageChannelMembers
        ));
        roleRepository.save(rTeamLead);

        Role rDev = getOrCreateRole("DEVELOPER", "Focused developer, Kanban workspace, progress sliders, and blockers", true);
        rDev.getPermissions().addAll(Set.of(
                pViewProject,
                pViewTask, pEditTask
        ));
        roleRepository.save(rDev);

        Role rSqa = getOrCreateRole("SQA", "Software Quality Assurance and review validation", true);
        rSqa.getPermissions().addAll(Set.of(
                pViewProject, pViewTask, pEditTask, pApproveTask
        ));
        roleRepository.save(rSqa);

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
                    .status("ACTIVE")
                    .startDate(LocalDate.now().minusDays(14))
                    .dueDate(LocalDate.now().plusDays(45))
                    .manager(manager)
                    .build());

            projectRepository.save(Project.builder()
                    .name("Biometric 2FA Engine")
                    .description("FIDO2 and WebAuthn hardware key verification service")
                    .status("ACTIVE")
                    .startDate(LocalDate.now().minusDays(5))
                    .dueDate(LocalDate.now().plusDays(30))
                    .manager(manager)
                    .build());

            projectRepository.save(Project.builder()
                    .name("Liquidity Settlement")
                    .description("Multi-currency Nostro/Vostro real-time liquidity balancing")
                    .status("ACTIVE")
                    .startDate(LocalDate.now().minusDays(20))
                    .dueDate(LocalDate.now().plusDays(60))
                    .manager(manager)
                    .build());

            // 5. Create Pipeline Stages for Core Payment Gateway
            Stage sBacklog = stageRepository.save(Stage.builder().project(p1).name("Backlog").position(0).isDoneStage(false).build());
            Stage sDb = stageRepository.save(Stage.builder().project(p1).name("DB Design").position(1).isDoneStage(false).build());
            Stage sApi = stageRepository.save(Stage.builder().project(p1).name("API Dev").position(2).isDoneStage(false).build());
            Stage sUi = stageRepository.save(Stage.builder().project(p1).name("UI & Layout").position(3).isDoneStage(false).build());
            Stage sReview = stageRepository.save(Stage.builder().project(p1).name("Review / QA").position(4).isDoneStage(false).build());
            Stage sDone = stageRepository.save(Stage.builder().project(p1).name("Done").position(5).isDoneStage(true).build());

            // 6. Create Tasks for Core Payment Gateway
            taskRepository.save(Task.builder()
                    .project(p1).stage(sBacklog).title("Implement Biometric 2FA Hook")
                    .description("Integrate FIDO2 challenge-response verification for transactions")
                    .priority("HIGH").progressPercentage(0).assignee(dev).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sBacklog).title("Audit Trail Export to CSV")
                    .description("Generate encrypted compliance audit logs for central bank audit")
                    .priority("LOW").progressPercentage(0).assignee(sqa).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sDb).title("MSSQL Partitioning for Ledger")
                    .description("Implement year-based table partitioning on transactions table")
                    .priority("CRITICAL").progressPercentage(30).assignee(dev).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sApi).title("WebSocket STOMP Broker & Event Hook")
                    .description("Enable live drag-and-drop and real-time blocker synchronization")
                    .priority("HIGH").progressPercentage(65).assignee(lead)
                    .isBlocked(true).blockerReason("Waiting for SSL certificate handshake setup").build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sUi).title("Emerald Reserve Design Tokens")
                    .description("Refactor CSS tokens for bank primary #006022 and gold accents")
                    .priority("CRITICAL").progressPercentage(90).assignee(dev).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sReview).title("JWT Refresh Filter Test Suite")
                    .description("Execute integration test suite covering expired and revoked tokens")
                    .priority("MEDIUM").progressPercentage(95).assignee(sqa).isBlocked(false).build());

            taskRepository.save(Task.builder()
                    .project(p1).stage(sDone).title("Spring Boot 3.3.5 & Java 21 Loom Scaffolding")
                    .description("Scaffold core backend architecture with virtual threads enabled")
                    .priority("HIGH").progressPercentage(100).assignee(dev).isBlocked(false).build());

            log.info("Created Seed Project, Stages, and Tasks!");
        }

        // 7. Create Channels if none exist
        if (channelRepository.count() == 0) {
            Channel chGeneral = channelRepository.save(Channel.builder().name("general").type("TEXT").createdBy(admin).build());
            Channel chApi = channelRepository.save(Channel.builder().name("core-banking-api").type("TEXT").createdBy(lead).build());
            channelRepository.save(Channel.builder().name("design-tokens").type("TEXT").createdBy(dev).build());
            channelRepository.save(Channel.builder().name("database-migration").type("TEXT").createdBy(lead).build());

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
                    .type("TASK_ASSIGNED")
                    .priority("CRITICAL")
                    .isRead(false)
                    .actionType("NAVIGATE_TASK")
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
                    .type("TASK_DUE_SOON")
                    .priority("WARNING")
                    .isRead(false)
                    .actionType("NAVIGATE_TASK")
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
                    .type("CHANNEL_MENTION")
                    .priority("INFO")
                    .isRead(false)
                    .actionType("NAVIGATE_CHANNEL")
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
                    .type("TASK_OVERDUE")
                    .priority("CRITICAL")
                    .isRead(false)
                    .actionType("NAVIGATE_TASK")
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
                    .type("CHANNEL_MENTION")
                    .priority("INFO")
                    .isRead(false)
                    .actionType("NAVIGATE_CHANNEL")
                    .referenceId(1L)
                    .senderName("Tariq Manager")
                    .senderAvatar("TM")
                    .createdAt(now.minusSeconds(2400))
                    .build());

            // Blocker Alert for Admin, Manager, Lead
            notificationRepository.save(Notification.builder()
                    .targetRole(rAdmin)
                    .title("Sprint Blocker Alert: WebSocket STOMP Broker & Event Hook")
                    .message("Arman Lead reported a blocker on 'WebSocket STOMP Broker & Event Hook': Waiting for SSL certificate handshake setup.")
                    .type("BLOCKER_ALERT")
                    .priority("CRITICAL")
                    .isRead(false)
                    .actionType("NAVIGATE_TASK")
                    .referenceId(4L)
                    .senderName("Arman Lead")
                    .senderAvatar("AL")
                    .createdAt(now.minusSeconds(1200))
                    .build());

            // Assignment for SQA
            notificationRepository.save(Notification.builder()
                    .recipient(sqa)
                    .title("New Task Assigned: JWT Refresh Filter Test Suite")
                    .message("Arman Lead assigned you to 'JWT Refresh Filter Test Suite' (MEDIUM priority).")
                    .type("TASK_ASSIGNED")
                    .priority("INFO")
                    .isRead(false)
                    .actionType("NAVIGATE_TASK")
                    .referenceId(6L)
                    .senderName("Arman Lead")
                    .senderAvatar("AL")
                    .createdAt(now.minusSeconds(5000))
                    .build());

            log.info("Seeded Initial Bank Role Notifications!");
        }

        log.info("SyncFlow Bank Enterprise initialization complete!");
    }

    private Permission getOrCreatePermission(String module, String action, String description) {
        return permissionRepository.findByModuleAndAction(module, action)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder()
                                .module(module)
                                .action(action)
                                .description(description)
                                .build()
                        ));
    }

    private Role getOrCreateRole(String name, String description, boolean isSystemRole) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(name)
                                .description(description)
                                .isSystemRole(isSystemRole)
                                .status(true)
                                .permissions(new HashSet<>())
                                .build()
                        ));
    }

    private User createUserIfNotFound(String email, String rawPassword, String fullName, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .status("ACTIVE")
                    .onLeave(false)
                    .role(role)
                    .build();
            User saved = userRepository.save(user);
            log.info("Created Seed User: {} with Role: {}", email, role.getName());
            return saved;
        });
    }
}
