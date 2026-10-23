package com.syncflow.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error body returned by every REST endpoint.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details
) {
}
