package com.syncflow.common.domain;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message, List.of());
    }
}
