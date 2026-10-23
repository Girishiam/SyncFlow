package com.syncflow.model.enums;

import com.syncflow.common.domain.DomainException;

import java.util.Arrays;
import java.util.Locale;

public enum DependencyType {
    BLOCKS, BLOCKED_BY, RELATES_TO;

    /** The same link seen from the other task. */
    public DependencyType inverse() {
        return switch (this) {
            case BLOCKS -> BLOCKED_BY;
            case BLOCKED_BY -> BLOCKS;
            case RELATES_TO -> RELATES_TO;
        };
    }

    /** Parses user input case-insensitively; null means BLOCKS. */
    public static DependencyType parse(String value) {
        if (value == null || value.isBlank()) return BLOCKS;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DomainException("Dependency type must be one of " + Arrays.toString(values()));
        }
    }
}
