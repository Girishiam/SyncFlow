package com.syncflow.auth;

import com.syncflow.audit.application.port.in.AuditLogUseCase;
import com.syncflow.auth.application.port.out.RoleRepositoryPort;
import com.syncflow.auth.application.port.out.UserRepositoryPort;
import com.syncflow.auth.application.service.AuthApplicationService;
import com.syncflow.common.domain.DomainException;
import com.syncflow.config.SyncFlowProperties;
import com.syncflow.model.RefreshToken;
import com.syncflow.model.User;
import com.syncflow.auth.application.port.out.RefreshTokenRepositoryPort;
import com.syncflow.security.CurrentUser;
import com.syncflow.security.JwtUtils;
import com.syncflow.security.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RefreshTokenReuseTest {

    @Test
    void replayingARevokedTokenRevokesAllSessionsOfTheUser() {
        RefreshTokenRepositoryPort tokens = mock(RefreshTokenRepositoryPort.class);
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("dev@example.com").build();
        RefreshToken revoked = RefreshToken.builder().user(user).tokenHash("h")
                .expiresAt(Instant.now().plusSeconds(3600)).revokedAt(Instant.now().minusSeconds(5)).build();
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        AuthApplicationService service = new AuthApplicationService(
                mock(AuthenticationManager.class), mock(JwtUtils.class), mock(UserRepositoryPort.class),
                mock(RoleRepositoryPort.class), tokens,
                new LoginAttemptService(new SyncFlowProperties()), mock(PasswordEncoder.class),
                mock(CurrentUser.class), mock(AuditLogUseCase.class), new SyncFlowProperties());

        assertThatThrownBy(() -> service.refresh("stolen-token"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Session expired");
        verify(tokens).revokeAllForUser(eq(userId), any(Instant.class));
        verify(tokens, never()).save(any());
    }
}
