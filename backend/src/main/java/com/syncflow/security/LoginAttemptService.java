package com.syncflow.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.syncflow.common.domain.DomainException;
import com.syncflow.config.SyncFlowProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Throttles password attempts per account (login and privilege elevation) to slow down
 * credential-guessing attacks. State is in-memory and per-instance.
 */
@Service
public class LoginAttemptService {

    private final Cache<String, AtomicInteger> failures;
    private final int maxAttempts;
    private final int lockoutMinutes;

    public LoginAttemptService(SyncFlowProperties properties) {
        this.maxAttempts = properties.getSecurity().getMaxFailedLoginAttempts();
        this.lockoutMinutes = properties.getSecurity().getLockoutMinutes();
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(lockoutMinutes))
                .maximumSize(100_000)
                .build();
    }

    public void assertNotLocked(String key) {
        AtomicInteger count = failures.getIfPresent(normalize(key));
        if (count != null && count.get() >= maxAttempts) {
            throw new TooManyAttemptsException(lockoutMinutes);
        }
    }

    public void recordFailure(String key) {
        failures.asMap().computeIfAbsent(normalize(key), k -> new AtomicInteger()).incrementAndGet();
    }

    public void recordSuccess(String key) {
        failures.invalidate(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    public static class TooManyAttemptsException extends DomainException {
        public TooManyAttemptsException(int minutes) {
            super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                    "Too many failed attempts. Try again in " + minutes + " minutes.", List.of());
        }
    }
}
