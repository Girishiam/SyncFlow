package com.syncflow.config;

import com.syncflow.model.enums.*;

import com.syncflow.model.Permission;
import com.syncflow.model.Role;
import com.syncflow.model.User;
import com.syncflow.repository.PermissionRepository;
import com.syncflow.repository.RoleRepository;
import com.syncflow.repository.UserRepository;
import com.syncflow.security.Permissions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Ensures the permission catalogue and system roles exist in every environment.
 *
 * <p>Default grants are applied only when a role or a permission is first created, so changes an
 * administrator makes to the permission matrix survive restarts, while permissions introduced by
 * new releases still reach the roles that should have them.</p>
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataInitializer implements CommandLineRunner {

    static final String ADMIN = Permissions.ADMIN_ROLE;
    static final String MANAGER = "REPORTING_MANAGER";
    static final String LEAD = "TEAM_LEAD";
    static final String DEV = "DEVELOPER";
    static final String SQA = "SQA";

    private record RoleDef(String name, String description) {
    }

    private record PermissionDef(String module, String action, String description, Set<String> defaultRoles) {
    }

    private static final List<RoleDef> ROLES = List.of(
            new RoleDef(ADMIN, "System Administrator with full master control"),
            new RoleDef(MANAGER, "Executive control, project lifecycle, and performance analytics"),
            new RoleDef(LEAD, "Squad leader, pipeline stage builder, review queue, and task assignment"),
            new RoleDef(DEV, "Focused developer, Kanban workspace, progress sliders, and blockers"),
            new RoleDef(SQA, "Software Quality Assurance and review validation")
    );

    private static final List<PermissionDef> PERMISSIONS = List.of(
            perm("PROJECT", "CREATE", "Create new enterprise project", MANAGER),
            perm("PROJECT", "VIEW", "View project details and pipeline", MANAGER, LEAD, DEV, SQA),
            perm("PROJECT", "EDIT", "Edit project parameters, automations and webhooks", MANAGER, LEAD),
            perm("PROJECT", "DELETE", "Delete project"),
            perm("TASK", "CREATE", "Create task", MANAGER, LEAD),
            perm("TASK", "VIEW", "View task", MANAGER, LEAD, DEV, SQA),
            perm("TASK", "EDIT", "Edit task and progress slider", MANAGER, LEAD, DEV, SQA),
            perm("TASK", "DELETE", "Delete task"),
            perm("TASK", "APPROVE", "Approve completed task in review", MANAGER, LEAD, SQA),
            perm("TASK", "ASSIGN", "Assign tasks to developers", MANAGER, LEAD),
            perm("TASK", "OVERRIDE_GATE", "Override workflow quality gates (requires password re-entry)", MANAGER),
            perm("STAGE", "BUILD", "Build and reorder pipeline stages", MANAGER, LEAD),
            perm("SPRINT", "MANAGE", "Create, start and close sprints", MANAGER, LEAD),
            perm("FINANCE", "VIEW", "View project budgets and cost reports", MANAGER),
            perm("AUDIT", "VIEW", "View and export the compliance audit ledger", MANAGER),
            perm("MEETING", "HOST", "Host instant department or squad meeting", MANAGER, LEAD),
            perm("CHAT", "CREATE_CHANNEL", "Create enterprise discussion channels", MANAGER, LEAD),
            perm("CHAT", "EDIT_CHANNEL", "Edit channel name, description, and icon/logo", MANAGER, LEAD),
            perm("CHAT", "DELETE_CHANNEL", "Delete enterprise discussion channels", MANAGER),
            perm("CHAT", "MANAGE_MEMBERS", "Add or remove members from channels", MANAGER, LEAD),
            perm("ADMIN", "MANAGE_USERS", "Manage users and overrides"),
            perm("ADMIN", "MANAGE_ROLES", "Manage roles and permission matrix")
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SyncFlowProperties properties;
    private final org.springframework.core.env.Environment environment;

    @Override
    @Transactional
    public void run(String... args) {
        Set<String> newRoles = new HashSet<>();
        Map<String, Role> roles = new LinkedHashMap<>();
        for (RoleDef def : ROLES) {
            Role role = roleRepository.findByName(def.name()).orElse(null);
            if (role == null) {
                role = roleRepository.save(Role.builder()
                        .name(def.name())
                        .description(def.description())
                        .isSystemRole(true)
                        .status(true)
                        .permissions(new HashSet<>())
                        .build());
                newRoles.add(def.name());
            }
            roles.put(def.name(), role);
        }

        for (PermissionDef def : PERMISSIONS) {
            Optional<Permission> existing = permissionRepository.findByModuleAndAction(def.module(), def.action());
            boolean isNewPermission = existing.isEmpty();
            Permission permission = existing.orElseGet(() -> permissionRepository.save(Permission.builder()
                    .module(def.module())
                    .action(def.action())
                    .description(def.description())
                    .build()));

            for (Map.Entry<String, Role> entry : roles.entrySet()) {
                boolean grantedByDefault = ADMIN.equals(entry.getKey()) || def.defaultRoles().contains(entry.getKey());
                if (grantedByDefault && (isNewPermission || newRoles.contains(entry.getKey()))) {
                    entry.getValue().getPermissions().add(permission);
                }
            }
        }
        roleRepository.saveAll(roles.values());

        bootstrapAdmin(roles.get(ADMIN));
        log.info("Reference data ready: {} permissions, {} system roles", PERMISSIONS.size(), ROLES.size());
    }

    private void bootstrapAdmin(Role adminRole) {
        if (userRepository.count() > 0) {
            return;
        }
        SyncFlowProperties.Bootstrap bootstrap = properties.getBootstrap();
        if (isBlank(bootstrap.getAdminEmail()) || isBlank(bootstrap.getAdminPassword())) {
            if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))) {
                return; // DemoDataSeeder creates the demo accounts
            }
            log.warn("No users exist. Set SYNCFLOW_ADMIN_EMAIL and SYNCFLOW_ADMIN_PASSWORD to create the first " +
                    "administrator, or run with the 'dev' profile to load demo accounts.");
            return;
        }
        userRepository.save(User.builder()
                .email(bootstrap.getAdminEmail().trim())
                .passwordHash(passwordEncoder.encode(bootstrap.getAdminPassword()))
                .fullName(bootstrap.getAdminName())
                .status(UserStatus.ACTIVE)
                .onLeave(false)
                .role(adminRole)
                .build());
        log.info("Created bootstrap administrator {}", bootstrap.getAdminEmail());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static PermissionDef perm(String module, String action, String description, String... roles) {
        return new PermissionDef(module, action, description, Set.of(roles));
    }
}
