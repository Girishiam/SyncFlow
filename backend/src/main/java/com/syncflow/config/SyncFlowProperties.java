package com.syncflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed view of the {@code syncflow.*} configuration tree.
 */
@ConfigurationProperties(prefix = "syncflow")
@Validated
@Getter
@Setter
public class SyncFlowProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Storage storage = new Storage();
    private final Webhooks webhooks = new Webhooks();
    private final Finance finance = new Finance();
    private final Capacity capacity = new Capacity();
    private final Bootstrap bootstrap = new Bootstrap();
    private final Security security = new Security();

    @Getter
    @Setter
    public static class Jwt {
        /** Base64-encoded HMAC key, at least 256 bits. Must be supplied via environment. */
        private String secret;
        private long accessExpirationMs = 15 * 60 * 1000L;
        private long refreshExpirationMs = 7 * 24 * 60 * 60 * 1000L;
        private long elevationExpirationMs = 5 * 60 * 1000L;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:4200"));
    }

    @Getter
    @Setter
    public static class Storage {
        private String attachmentsDir = "./data/attachments";
    }

    @Getter
    @Setter
    public static class Webhooks {
        /** Allow deliveries to loopback / private-network addresses (useful in development only). */
        private boolean allowPrivateTargets = false;
        private int connectTimeoutMs = 5000;
        private int requestTimeoutMs = 10000;
        private int maxAttempts = 3;
    }

    @Getter
    @Setter
    public static class Finance {
        /** Hourly cost rate keyed by role name. */
        private Map<String, Double> roleHourlyRates = new LinkedHashMap<>();
        private double defaultHourlyRate = 85.0;
    }

    @Getter
    @Setter
    public static class Capacity {
        private int defaultStoryPointsPerMember = 15;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        /** When set and no users exist, an ADMIN account is created on startup. */
        private String adminEmail;
        private String adminPassword;
        private String adminName = "System Administrator";
    }

    @Getter
    @Setter
    public static class Security {
        private int maxFailedLoginAttempts = 5;
        private int lockoutMinutes = 15;
    }
}
