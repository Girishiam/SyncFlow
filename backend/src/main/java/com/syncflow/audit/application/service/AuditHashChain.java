package com.syncflow.audit.application.service;

import com.syncflow.model.AuditLog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Tamper-evident hash chain for the audit ledger: each record's hash covers its own content and the
 * hash of the previous record, so editing or deleting any record breaks every later link.
 */
public final class AuditHashChain {

    private static final char SEPARATOR = '\u001F';

    private AuditHashChain() {
    }

    public static String hash(AuditLog log, String previousHash) {
        String canonical = String.join(String.valueOf(SEPARATOR),
                Objects.toString(previousHash, ""),
                Objects.toString(log.getAction(), ""),
                Objects.toString(log.getEntityType(), ""),
                Objects.toString(log.getEntityId(), ""),
                Objects.toString(log.getEntityName(), ""),
                Objects.toString(log.getActorName(), ""),
                Objects.toString(log.getActorEmail(), ""),
                Objects.toString(log.getActorRole(), ""),
                Objects.toString(log.getDetails(), ""),
                Objects.toString(log.getIpAddress(), ""),
                Objects.toString(log.getTimestamp(), ""));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Records sealed by this chain carry a full 64-char hex digest; older seed records do not. */
    public static boolean isChained(AuditLog log) {
        return log.getVerificationHash() != null && log.getVerificationHash().length() == 64;
    }

    public record VerificationResult(long verifiedRecords, long brokenRecords, long legacyRecords, Long firstBrokenId) {
    }

    /** Streaming verifier: feed records in ascending id order. */
    public static final class Verifier {
        private String previousHash;
        private long verified;
        private long broken;
        private long legacy;
        private Long firstBrokenId;
        private Long lastId;

        public Verifier() {
        }

        /** Resumes verification after a checkpoint. */
        public Verifier(String previousHash, long verified, long broken, long legacy, Long firstBrokenId) {
            this.previousHash = previousHash;
            this.verified = verified;
            this.broken = broken;
            this.legacy = legacy;
            this.firstBrokenId = firstBrokenId;
        }

        public String lastHash() {
            return previousHash;
        }

        public Long lastId() {
            return lastId;
        }

        public void accept(AuditLog log) {
            if (!isChained(log)) {
                legacy++;
            } else {
                boolean linkOk = Objects.equals(log.getPreviousHash(), previousHash);
                boolean contentOk = hash(log, log.getPreviousHash()).equals(log.getVerificationHash());
                if (linkOk && contentOk) {
                    verified++;
                } else {
                    broken++;
                    if (firstBrokenId == null) firstBrokenId = log.getId();
                }
            }
            previousHash = log.getVerificationHash();
            lastId = log.getId();
        }

        public VerificationResult result() {
            return new VerificationResult(verified, broken, legacy, firstBrokenId);
        }
    }
}
