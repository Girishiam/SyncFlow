package com.syncflow.auth.application.port.in;

import com.syncflow.dto.AuthResponse;
import com.syncflow.dto.LoginRequest;
import com.syncflow.dto.UserDTO;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AuthUseCase {
    AuthResponse login(LoginRequest loginRequest);
    UserDTO getCurrentUser(Authentication authentication);
    List<UserDTO> getAllUsers();
}
