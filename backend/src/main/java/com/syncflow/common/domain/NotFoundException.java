package com.syncflow.common.domain;

import org.springframework.http.HttpStatus;

import java.util.List;

public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message, List.of());
    }
}
