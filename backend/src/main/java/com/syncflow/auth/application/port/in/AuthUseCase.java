package com.syncflow.auth.application.port.in;

import com.syncflow.dto.AuthResponse;
import com.syncflow.dto.LoginRequest;
import com.syncflow.dto.UserDTO;
import org.springframework.security.core.Authentication;

public interface AuthUseCase {
    AuthResponse login(LoginRequest loginRequest);
    UserDTO getCurrentUser(Authentication authentication);
}
