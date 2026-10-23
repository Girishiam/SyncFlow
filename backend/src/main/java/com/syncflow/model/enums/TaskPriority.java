package com.syncflow.model.enums;

import com.syncflow.common.domain.DomainException;

import java.util.Arrays;
import java.util.Locale;

public enum TaskPriority {
    LOW, MEDIUM, HIGH, CRITICAL;

    /** Parses user input case-insensitively; null means MEDIUM. */
    public static TaskPriority parse(String value) {
        if (value == null || value.isBlank()) return MEDIUM;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DomainException("Priority must be one of " + Arrays.toString(values()));
        }
    }
}
