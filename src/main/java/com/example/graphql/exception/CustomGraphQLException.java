/**
 * Custom exception for GraphQL errors.
 * Used to provide structured error responses to clients.
 */
package com.example.graphql.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * A custom, structured exception designed for use within GraphQL resolvers.
 *
 * <p>This exception enriches a standard {@link RuntimeException} with additional metadata, such as
 * status codes, error titles, and detailed error messages. Intended for use with a global GraphQL
 * exception handler to produce consistent, client-friendly error responses.
 */
public class CustomGraphQLException extends RuntimeException {

    private final int statusCode; // HTTP-like status (400, 404, etc.)
    private final String error; // Short, descriptive error title
    private final String path; // GraphQL path where error occurred
    private final List<String> details; // Specific validation or error details
    private final Instant timestamp; // Error occurrence time

    /**
     * Constructs a new CustomGraphQLException with detailed error information.
     *
     * @param statusCode HTTP-like status code for the error.
     * @param error Short descriptive error title (e.g., "Not Found").
     * @param message Main error message (passed to RuntimeException).
     * @param path GraphQL path where the error occurred.
     * @param details List of detailed error messages.
     */
    public CustomGraphQLException(int statusCode, String error, String message, String path, List<String> details) {
        super(message);
        this.statusCode = statusCode;
        this.error = error;
        this.path = path;
        this.details = details != null ? Collections.unmodifiableList(details) : List.of();
        this.timestamp = Instant.now();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

    public String getPath() {
        return path;
    }

    public List<String> getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
