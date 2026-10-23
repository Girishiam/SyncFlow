package com.syncflow.common.domain;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base exception for business-rule failures. Carries the HTTP status and a stable machine-readable
 * code so the web layer can translate it into a structured error response.
 */
public class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;

    public DomainException(String message) {
        this(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, List.of());
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.BAD_REQUEST;
        this.code = "BAD_REQUEST";
        this.details = List.of();
    }

    protected DomainException(HttpStatus status, String code, String message, List<String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details != null ? List.copyOf(details) : List.of();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
