package com.syncflow.auth.application.service;

import com.syncflow.auth.application.port.in.AuthUseCase;
import com.syncflow.common.domain.DomainException;
import com.syncflow.dto.AuthResponse;
import com.syncflow.dto.LoginRequest;
import com.syncflow.dto.UserDTO;
import com.syncflow.model.User;
import com.syncflow.security.JwtUtils;
import com.syncflow.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import com.syncflow.auth.application.port.out.RoleRepositoryPort;
import com.syncflow.auth.application.port.out.UserRepositoryPort;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService implements AuthUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(loginRequest.getEmail());

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .onLeave(user.getOnLeave())
                .leaveNote(user.getLeaveNote())
                .role(user.getRole())
                .permissions(userDetails.getPermissionStrings())
                .build();

        return AuthResponse.builder()
                .token(jwt)
                .user(userDTO)
                .build();
    }

    @Override
    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new DomainException("Unauthorized: No valid session found");
        }

        User user = userDetails.getUser();
        return toDTO(user, userDetails.getPermissionStrings());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepositoryPort.findAll().stream()
                .map(u -> toDTO(u, java.util.Collections.emptySet()))
                .toList();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public UserDTO updateUserRole(java.util.UUID userId, Long roleId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new DomainException("User not found with ID: " + userId));

        com.syncflow.model.Role role = roleRepositoryPort.findById(roleId)
                .orElseThrow(() -> new DomainException("Role not found with ID: " + roleId));

        user.setRole(role);
        User saved = userRepositoryPort.save(user);
        log.info("Updated role for user: {} to: {}", saved.getEmail(), role.getName());
        return toDTO(saved, java.util.Collections.emptySet());
    }

    private UserDTO toDTO(User user, java.util.Set<String> permissions) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .onLeave(user.getOnLeave())
                .leaveNote(user.getLeaveNote())
                .role(user.getRole())
                .permissions(permissions)
                .build();
    }
}
