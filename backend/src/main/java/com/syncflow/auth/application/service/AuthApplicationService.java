package com.syncflow.auth.application.service;

import com.syncflow.auth.application.port.out.RefreshTokenRepositoryPort;

import com.syncflow.model.enums.*;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.auth.application.port.in.AuthUseCase;
import com.syncflow.auth.application.port.out.RoleRepositoryPort;
import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.common.domain.ConflictException;
import com.syncflow.common.domain.DomainException;
import com.syncflow.common.domain.ForbiddenException;
import com.syncflow.common.domain.NotFoundException;
import com.syncflow.config.SyncFlowProperties;
import com.syncflow.dto.*;
import com.syncflow.model.RefreshToken;
import com.syncflow.model.Role;
import com.syncflow.model.User;
import com.syncflow.security.CurrentUser;
import com.syncflow.security.JwtUtils;
import com.syncflow.security.LoginAttemptService;
import com.syncflow.security.Permissions;
import com.syncflow.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService implements AuthUseCase {

    /** Privileged scopes that can be unlocked by re-entering the password, and the permission each requires. */
    public static final Map<String, String> ELEVATION_SCOPES = Map.of(
            "WORKFLOW_GATE_OVERRIDE", Permissions.TASK_OVERRIDE_GATE
    );

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditLogUseCase auditLogUseCase;
    private final SyncFlowProperties properties;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim();
        loginAttemptService.assertNotLocked(email);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            throw e;
        }
        loginAttemptService.recordSuccess(email);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return issueTokens(userDetails.getUser(), userDetails.getPermissionStrings());
    }

    /** Rejections must not roll back the reuse-detection revocation performed before throwing. */
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.getRevokedAt() != null) {
            // A revoked token being replayed means it was likely stolen: invalidate the whole session family.
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), Instant.now());
            log.warn("Refresh token reuse detected for user {}; all sessions revoked", stored.getUser().getEmail());
            throw new InvalidRefreshTokenException();
        }
        if (!stored.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        User user = stored.getUser();
        UserDetailsImpl details = UserDetailsImpl.build(user);
        if (!details.isEnabled() || !details.isAccountNonLocked()) {
            throw new InvalidRefreshTokenException();
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        return issueTokens(user, details.getPermissionStrings());
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            }
        });
    }

    @Override
    @Transactional
    public ElevationResponse elevate(ElevationRequest request) {
        UserDetailsImpl user = currentUser.require();
        String scope = request.getScope().trim().toUpperCase(Locale.ROOT);
        String requiredPermission = ELEVATION_SCOPES.get(scope);
        if (requiredPermission == null) {
            throw new DomainException("Unknown elevation scope: " + scope);
        }
        if (!CurrentUser.has(user, requiredPermission)) {
            throw new ForbiddenException("Your role is not allowed to perform " + scope);
        }

        String throttleKey = "elevate:" + user.getEmail();
        loginAttemptService.assertNotLocked(throttleKey);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(throttleKey);
            auditLogUseCase.recordLog("ELEVATION_FAILED", "SECURITY", null, scope, null, null, null,
                    "Password re-authentication failed for privileged scope " + scope);
            throw new BadCredentialsException("Password confirmation failed");
        }
        loginAttemptService.recordSuccess(throttleKey);

        auditLogUseCase.recordLog("ELEVATION_GRANTED", "SECURITY", null, scope, null, null, null,
                "Password re-authentication succeeded; elevated privileges granted for " + scope);

        return ElevationResponse.builder()
                .elevationToken(jwtUtils.generateElevationToken(user.getEmail(), scope))
                .scope(scope)
                .expiresAt(jwtUtils.elevationExpiry())
                .build();
    }

    @Override
    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new ForbiddenException("No valid session found");
        }
        return toDTO(userDetails.getUser(), userDetails.getPermissionStrings());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepositoryPort.findAll().stream()
                .map(u -> toDTO(u, Collections.emptySet()))
                .toList();
    }

    @Override
    @Transactional
    public UserDTO updateUserRole(UUID userId, Long roleId) {
        UserDetailsImpl actor = currentUser.require();
        if (actor.getId().equals(userId)) {
            throw new ForbiddenException("You cannot change your own role");
        }

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        Role role = roleRepositoryPort.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found with ID: " + roleId));

        if (!Boolean.TRUE.equals(role.getStatus())) {
            throw new ConflictException("Role " + role.getName() + " is deactivated and cannot be assigned");
        }
        boolean demotingAdmin = user.getRole() != null
                && Permissions.ADMIN_ROLE.equalsIgnoreCase(user.getRole().getName())
                && !Permissions.ADMIN_ROLE.equalsIgnoreCase(role.getName());
        if (demotingAdmin && userRepositoryPort.countByRoleId(user.getRole().getId()) <= 1) {
            throw new ConflictException("Cannot remove the last administrator");
        }

        String previousRole = user.getRole() != null ? user.getRole().getName() : "NONE";
        user.setRole(role);
        User saved = userRepositoryPort.save(user);

        auditLogUseCase.recordLog("ROLE_ASSIGNED", "USER", null, saved.getEmail(), null, null, null,
                "Changed role of " + saved.getEmail() + " from " + previousRole + " to " + role.getName());
        log.info("Updated role for user: {} to: {}", saved.getEmail(), role.getName());
        return toDTO(saved, Collections.emptySet());
    }

    /** Housekeeping: drop refresh tokens that expired more than a day ago. */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int removed = refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }

    private AuthResponse issueTokens(User user, Set<String> permissions) {
        String refreshToken = newOpaqueToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(refreshToken))
                .expiresAt(Instant.now().plusMillis(properties.getJwt().getRefreshExpirationMs()))
                .build());

        return AuthResponse.builder()
                .token(jwtUtils.generateAccessToken(user.getEmail()))
                .expiresIn(jwtUtils.getAccessExpirationMs() / 1000)
                .refreshToken(refreshToken)
                .user(toDTO(user, permissions))
                .build();
    }

    private static String newOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private UserDTO toDTO(User user, Set<String> permissions) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus().name())
                .onLeave(user.getOnLeave())
                .leaveNote(user.getLeaveNote())
                .role(user.getRole())
                .permissions(permissions)
                .build();
    }

    static class InvalidRefreshTokenException extends DomainException {
        InvalidRefreshTokenException() {
            super(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Session expired. Please sign in again.", List.of());
        }
    }
}
