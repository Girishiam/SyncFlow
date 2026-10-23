package com.syncflow.auth.application.service;

import com.syncflow.auth.application.port.out.UserRepositoryPort;

import com.syncflow.auth.application.port.in.RbacUseCase;
import com.syncflow.auth.application.port.out.PermissionRepositoryPort;
import com.syncflow.auth.application.port.out.RoleRepositoryPort;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.CreateRoleRequest;
import com.syncflow.dto.PermissionDTO;
import com.syncflow.dto.RoleDTO;
import com.syncflow.dto.UpdateRolePermissionsRequest;
import com.syncflow.model.Permission;
import com.syncflow.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RbacApplicationService implements RbacUseCase {

    private final RoleRepositoryPort roleRepositoryPort;
    private final PermissionRepositoryPort permissionRepositoryPort;
    private final UserRepositoryPort userRepository;
    private final com.syncflow.audit.application.port.in.AuditLogUseCase auditLogUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepositoryPort.findAll().stream()
                .map(this::toRoleDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDTO> getAllPermissions() {
        return permissionRepositoryPort.findAll().stream()
                .map(p -> PermissionDTO.builder()
                        .id(p.getId())
                        .module(p.getModule())
                        .action(p.getAction())
                        .description(p.getDescription())
                        .authority(p.getAuthorityString())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public RoleDTO updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepositoryPort.findById(roleId)
                .orElseThrow(() -> new com.syncflow.common.domain.NotFoundException("Role not found: " + roleId));

        List<Permission> permissions = permissionRepositoryPort.findAllById(request.getPermissionIds());
        role.setPermissions(new HashSet<>(permissions));

        Role saved = roleRepositoryPort.save(role);
        log.info("Updated permissions for Role: {} (total: {})", saved.getName(), saved.getPermissions().size());
        auditLogUseCase.recordLog("ROLE_UPDATED", "ROLE", saved.getId(), saved.getName(), null, null, null,
                "Permission set: " + saved.getPermissions().stream().map(Permission::getAuthorityString).sorted().toList());
        return toRoleDTO(saved);
    }

    @Override
    @Transactional
    public RoleDTO createCustomRole(CreateRoleRequest request) {
        String roleName = request.getName().trim().toUpperCase().replace(" ", "_");

        if (roleRepositoryPort.findByName(roleName).isPresent()) {
            throw new com.syncflow.common.domain.ConflictException("Role with name " + roleName + " already exists");
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions.addAll(permissionRepositoryPort.findAllById(request.getPermissionIds()));
        }

        Role role = Role.builder()
                .name(roleName)
                .description(request.getDescription())
                .isSystemRole(false)
                .permissions(permissions)
                .build();

        Role saved = roleRepositoryPort.save(role);
        log.info("Created custom role: {}", saved.getName());
        auditLogUseCase.recordLog("ROLE_CREATED", "ROLE", saved.getId(), saved.getName(), null, null, null,
                "Created with " + saved.getPermissions().size() + " permission(s)");
        return toRoleDTO(saved);
    }

    @Override
    @Transactional
    public RoleDTO toggleRoleStatus(Long roleId) {
        Role role = roleRepositoryPort.findById(roleId)
                .orElseThrow(() -> new com.syncflow.common.domain.NotFoundException("Role not found with ID: " + roleId));

        if (com.syncflow.security.Permissions.ADMIN_ROLE.equalsIgnoreCase(role.getName())) {
            throw new com.syncflow.common.domain.ConflictException("The ADMIN role cannot be deactivated");
        }
        boolean newStatus = role.getStatus() == null || !role.getStatus();
        role.setStatus(newStatus);
        Role saved = roleRepositoryPort.save(role);
        log.info("Toggled status for Role: {} to {}", saved.getName(), newStatus ? "ACTIVE" : "DEACTIVATED");
        auditLogUseCase.recordLog("ROLE_STATUS_CHANGED", "ROLE", saved.getId(), saved.getName(), null, null, null,
                newStatus ? "Role activated" : "Role deactivated; its members lose all role permissions");
        return toRoleDTO(saved);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        Role role = roleRepositoryPort.findById(roleId)
                .orElseThrow(() -> new com.syncflow.common.domain.NotFoundException("Role not found with ID: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new DomainException("System roles cannot be deleted.");
        }

        long assignedUsers = userRepository.countByRoleId(roleId);
        if (assignedUsers > 0) {
            throw new com.syncflow.common.domain.ConflictException("Cannot delete role '" + role.getName() + "' because " + assignedUsers + " bank member(s) are currently assigned to it. Please reassign those members first.");
        }

        roleRepositoryPort.deleteById(roleId);
        auditLogUseCase.recordLog("ROLE_DELETED", "ROLE", roleId, role.getName(), null, null, null, "Custom role deleted");
        log.info("Permanently deleted custom role: {}", role.getName());
    }

    private RoleDTO toRoleDTO(Role role) {
        List<Long> pIds = role.getPermissions().stream().map(Permission::getId).toList();
        List<String> pAuths = role.getPermissions().stream().map(Permission::getAuthorityString).toList();
        long usersCount = userRepository.countByRoleId(role.getId());

        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .status(role.getStatus() != null ? role.getStatus() : true)
                .assignedUsersCount(usersCount)
                .permissionIds(pIds)
                .permissionAuthorities(pAuths)
                .build();
    }
}
