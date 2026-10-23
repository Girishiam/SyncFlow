package com.syncflow.auth.adapter.web;

import com.syncflow.auth.application.port.in.AuthUseCase;
import com.syncflow.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authUseCase.login(loginRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authUseCase.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authUseCase.logout(request != null ? request.getRefreshToken() : null);
        return ResponseEntity.noContent().build();
    }

    /** Re-enter the password to obtain a short-lived token for a privileged action. */
    @PostMapping("/elevate")
    public ResponseEntity<ElevationResponse> elevate(@Valid @RequestBody ElevationRequest request) {
        return ResponseEntity.ok(authUseCase.elevate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(authUseCase.getCurrentUser(authentication));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(authUseCase.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("@access.can('ADMIN:MANAGE_USERS')")
    public ResponseEntity<UserDTO> updateUserRole(@PathVariable UUID id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(authUseCase.updateUserRole(id, request.getRoleId()));
    }

    @lombok.Data
    public static class UpdateUserRoleRequest {
        @NotNull
        private Long roleId;
    }
}
