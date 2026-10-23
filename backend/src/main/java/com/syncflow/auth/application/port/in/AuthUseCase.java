package com.syncflow.auth.application.port.in;

import com.syncflow.dto.AuthResponse;
import com.syncflow.dto.ElevationRequest;
import com.syncflow.dto.ElevationResponse;
import com.syncflow.dto.LoginRequest;
import com.syncflow.dto.UserDTO;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

public interface AuthUseCase {
    AuthResponse login(LoginRequest loginRequest);

    AuthResponse refresh(String refreshToken);

    void logout(String refreshToken);

    ElevationResponse elevate(ElevationRequest request);

    UserDTO getCurrentUser(Authentication authentication);

    List<UserDTO> getAllUsers();

    UserDTO updateUserRole(UUID userId, Long roleId);
}
