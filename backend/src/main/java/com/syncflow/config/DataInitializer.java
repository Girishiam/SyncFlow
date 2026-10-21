package com.syncflow.config;

import com.syncflow.model.Permission;
import com.syncflow.model.Role;
import com.syncflow.model.User;
import com.syncflow.repository.PermissionRepository;
import com.syncflow.repository.RoleRepository;
import com.syncflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
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
        Permission pCreateChannel = getOrCreatePermission("CHAT", "CREATE_CHANNEL", "Create Discord-style channels");

        Permission pManageUsers = getOrCreatePermission("ADMIN", "MANAGE_USERS", "Manage users and overrides");
        Permission pManageRoles = getOrCreatePermission("ADMIN", "MANAGE_ROLES", "Manage roles and permission matrix");

        // 2. Create Roles
        Role rAdmin = getOrCreateRole("ADMIN", "System Administrator with full master control", true);
        rAdmin.getPermissions().addAll(Set.of(
                pCreateProject, pViewProject, pEditProject, pDeleteProject,
                pCreateTask, pViewTask, pEditTask, pDeleteTask, pApproveTask, pAssignTask,
                pBuildStage, pHostMeeting, pCreateChannel, pManageUsers, pManageRoles
        ));
        roleRepository.save(rAdmin);

        Role rManager = getOrCreateRole("REPORTING_MANAGER", "Executive control, project lifecycle, and performance analytics", true);
        rManager.getPermissions().addAll(Set.of(
                pCreateProject, pViewProject, pEditProject,
                pCreateTask, pViewTask, pEditTask, pApproveTask, pAssignTask,
                pBuildStage, pHostMeeting, pCreateChannel
        ));
        roleRepository.save(rManager);

        Role rTeamLead = getOrCreateRole("TEAM_LEAD", "Squad leader, pipeline stage builder, review queue, and task assignment", true);
        rTeamLead.getPermissions().addAll(Set.of(
                pViewProject, pEditProject,
                pCreateTask, pViewTask, pEditTask, pApproveTask, pAssignTask,
                pBuildStage, pHostMeeting, pCreateChannel
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
        createUserIfNotFound("admin@syncflow.bank", "Admin@123", "Chief Admin", rAdmin);
        createUserIfNotFound("manager@syncflow.bank", "Manager@123", "Tariq Manager", rManager);
        createUserIfNotFound("lead@syncflow.bank", "Lead@123", "Arman Lead", rTeamLead);
        createUserIfNotFound("dev@syncflow.bank", "Dev@123", "Farhan Senior Dev", rDev);
        createUserIfNotFound("sqa@syncflow.bank", "Sqa@123", "Sarah SQA Engineer", rSqa);

        log.info("SyncFlow Bank Enterprise RBAC initialization complete!");
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
                                .permissions(new HashSet<>())
                                .build()
                ));
    }

    private void createUserIfNotFound(String email, String rawPassword, String fullName, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .status("ACTIVE")
                    .onLeave(false)
                    .role(role)
                    .build();
            userRepository.save(user);
            log.info("Created Seed User: {} with Role: {}", email, role.getName());
        }
    }
}
