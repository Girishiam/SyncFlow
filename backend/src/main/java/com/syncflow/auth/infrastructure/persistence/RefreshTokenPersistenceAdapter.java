package com.syncflow.auth.infrastructure.persistence;

import com.syncflow.auth.application.port.out.RefreshTokenRepositoryPort;
import com.syncflow.repository.RefreshTokenRepository;
import com.syncflow.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenRepository repository;

    @Override
    public RefreshToken save(RefreshToken token) {
        return repository.save(token);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash);
    }

    @Override
    public int revokeAllForUser(UUID userId, Instant now) {
        return repository.revokeAllForUser(userId, now);
    }

    @Override
    public int deleteExpiredBefore(Instant cutoff) {
        return repository.deleteExpiredBefore(cutoff);
    }
}
