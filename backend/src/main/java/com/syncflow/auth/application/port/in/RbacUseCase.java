package com.syncflow.auth.application.port.in;

import com.syncflow.dto.CreateRoleRequest;
import com.syncflow.dto.PermissionDTO;
import com.syncflow.dto.RoleDTO;
import com.syncflow.dto.UpdateRolePermissionsRequest;

import java.util.List;

public interface RbacUseCase {
    List<RoleDTO> getAllRoles();
    List<PermissionDTO> getAllPermissions();
    RoleDTO updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request);
    RoleDTO createCustomRole(CreateRoleRequest request);
}
