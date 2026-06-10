package com.example.backendapi.shared;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable error response shared by all controllers.
 * A predictable shape lets clients display useful messages with one parsing strategy.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}
