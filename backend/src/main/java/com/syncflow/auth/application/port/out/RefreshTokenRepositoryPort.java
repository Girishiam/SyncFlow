package com.syncflow.auth.application.port.out;

import com.syncflow.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link RefreshToken}. */
public interface RefreshTokenRepositoryPort {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    int revokeAllForUser(UUID userId, Instant now);
    int deleteExpiredBefore(Instant cutoff);
}
