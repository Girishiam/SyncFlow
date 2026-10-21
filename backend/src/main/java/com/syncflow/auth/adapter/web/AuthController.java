package com.syncflow.auth.adapter.web;

import com.syncflow.auth.application.port.in.AuthUseCase;
import com.syncflow.dto.AuthResponse;
import com.syncflow.dto.LoginRequest;
import com.syncflow.dto.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authUseCase.login(loginRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(authUseCase.getCurrentUser(authentication));
    }

    @GetMapping("/users")
    public ResponseEntity<java.util.List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(authUseCase.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable java.util.UUID id,
            @RequestBody java.util.Map<String, Long> payload) {
        Long roleId = payload.get("roleId");
        return ResponseEntity.ok(authUseCase.updateUserRole(id, roleId));
    }
}
