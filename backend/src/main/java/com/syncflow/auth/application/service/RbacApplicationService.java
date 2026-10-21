package com.syncflow.auth.application.service;

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
                .orElseThrow(() -> new DomainException("Role not found: " + roleId));

        List<Permission> permissions = permissionRepositoryPort.findAllById(request.getPermissionIds());
        role.setPermissions(new HashSet<>(permissions));

        Role saved = roleRepositoryPort.save(role);
        log.info("Updated permissions for Role: {} (total: {})", saved.getName(), saved.getPermissions().size());
        return toRoleDTO(saved);
    }

    @Override
    @Transactional
    public RoleDTO createCustomRole(CreateRoleRequest request) {
        String roleName = request.getName().trim().toUpperCase().replace(" ", "_");

        if (roleRepositoryPort.findByName(roleName).isPresent()) {
            throw new DomainException("Role with name " + roleName + " already exists");
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
        return toRoleDTO(saved);
    }

    private RoleDTO toRoleDTO(Role role) {
        List<Long> pIds = role.getPermissions().stream().map(Permission::getId).toList();
        List<String> pAuths = role.getPermissions().stream().map(Permission::getAuthorityString).toList();

        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .permissionIds(pIds)
                .permissionAuthorities(pAuths)
                .build();
    }
}
