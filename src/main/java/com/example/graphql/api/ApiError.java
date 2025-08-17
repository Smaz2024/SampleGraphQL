package com.example.graphql.api;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/** Represents a standardized error response structure for the API. */
public class ApiError {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final Instant timestamp;
    private final List<String> details;

    private ApiError(int status, String error, String message, String path, List<String> details) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
        this.details = details != null ? List.copyOf(details) : Collections.emptyList();
    }

    // Static factory method for cleaner instantiation
    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(status, error, message, path, details);
    }

    // Overload when details are not required
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Collections.emptyList());
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<String> getDetails() {
        return details;
    }
}
