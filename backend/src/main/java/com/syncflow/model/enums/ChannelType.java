package com.syncflow.model.enums;

import java.util.Locale;

public enum ChannelType {
    TEXT, VOICE, ANNOUNCEMENT;

    /** Parses user input case-insensitively; null or unknown means TEXT. */
    public static ChannelType parseOrDefault(String value) {
        if (value == null || value.isBlank()) return TEXT;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}
