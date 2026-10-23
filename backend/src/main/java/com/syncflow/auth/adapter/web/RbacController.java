package com.syncflow.auth.adapter.web;

import com.syncflow.auth.application.port.in.RbacUseCase;
import com.syncflow.dto.CreateRoleRequest;
import com.syncflow.dto.PermissionDTO;
import com.syncflow.dto.RoleDTO;
import com.syncflow.dto.UpdateRolePermissionsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/rbac")
@RequiredArgsConstructor
@Slf4j
public class RbacController {

    private final RbacUseCase rbacUseCase;

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(rbacUseCase.getAllRoles());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        return ResponseEntity.ok(rbacUseCase.getAllPermissions());
    }

    @PutMapping("/roles/{id}/permissions")
    @org.springframework.security.access.prepost.PreAuthorize("@access.can('ADMIN:MANAGE_ROLES')")
    public ResponseEntity<RoleDTO> updateRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(rbacUseCase.updateRolePermissions(id, request));
    }

    @PostMapping("/roles")
    @org.springframework.security.access.prepost.PreAuthorize("@access.can('ADMIN:MANAGE_ROLES')")
    public ResponseEntity<RoleDTO> createCustomRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(rbacUseCase.createCustomRole(request));
    }

    @PutMapping("/roles/{id}/toggle-status")
    @org.springframework.security.access.prepost.PreAuthorize("@access.can('ADMIN:MANAGE_ROLES')")
    public ResponseEntity<RoleDTO> toggleRoleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(rbacUseCase.toggleRoleStatus(id));
    }

    @DeleteMapping("/roles/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@access.can('ADMIN:MANAGE_ROLES')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        rbacUseCase.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
