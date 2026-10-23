package com.syncflow.common.domain;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message, List.of());
    }

    public ConflictException(String code, String message, List<String> details) {
        super(HttpStatus.CONFLICT, code, message, details);
    }
}
