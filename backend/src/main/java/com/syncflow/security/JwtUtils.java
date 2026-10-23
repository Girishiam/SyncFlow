package com.syncflow.security;

import com.syncflow.config.SyncFlowProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies signed JWTs. Two token types exist:
 * <ul>
 *   <li>{@code access}: short-lived API bearer token</li>
 *   <li>{@code elevation}: very short-lived proof that the user re-entered their password for a
 *   specific privileged scope (e.g. overriding a workflow gate)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_ELEVATION = "elevation";
    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_SCOPE = "scope";

    private final SyncFlowProperties properties;
    private SecretKey key;

    @PostConstruct
    void init() {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "syncflow.jwt.secret is not configured. Set the SYNCFLOW_JWT_SECRET environment variable " +
                    "(e.g. `openssl rand -base64 48`) or add it to config/application-local.yml.");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("syncflow.jwt.secret must be Base64 encoded", e);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("syncflow.jwt.secret must decode to at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(String email) {
        return build(email, TYPE_ACCESS, null, properties.getJwt().getAccessExpirationMs());
    }

    public String generateElevationToken(String email, String scope) {
        return build(email, TYPE_ELEVATION, scope, properties.getJwt().getElevationExpirationMs());
    }

    public long getAccessExpirationMs() {
        return properties.getJwt().getAccessExpirationMs();
    }

    public Instant elevationExpiry() {
        return Instant.now().plusMillis(properties.getJwt().getElevationExpirationMs());
    }

    /** Returns the subject (email) when the token is a valid, unexpired access token. */
    public Optional<String> parseAccessToken(String token) {
        return parse(token)
                .filter(c -> TYPE_ACCESS.equals(c.get(CLAIM_TYPE, String.class)))
                .map(Claims::getSubject);
    }

    /** True when the token is a valid elevation token for exactly this user and scope. */
    public boolean isValidElevation(String token, String email, String scope) {
        if (token == null || token.isBlank() || email == null) {
            return false;
        }
        return parse(token)
                .filter(c -> TYPE_ELEVATION.equals(c.get(CLAIM_TYPE, String.class)))
                .filter(c -> email.equalsIgnoreCase(c.getSubject()))
                .filter(c -> scope.equals(c.get(CLAIM_SCOPE, String.class)))
                .isPresent();
    }

    private String build(String subject, String type, String scope, long ttlMs) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs));
        if (scope != null) {
            builder.claim(CLAIM_SCOPE, scope);
        }
        return builder.signWith(key).compact();
    }

    private Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
