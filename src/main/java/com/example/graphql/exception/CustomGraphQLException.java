package com.example.graphql.exception;

import java.time.Instant;
import java.util.List;

/**
 * A custom, structured exception designed for use within GraphQL resolvers.
 *
 * <p>This exception class enriches a standard {@link RuntimeException} with additional,
 * machine-readable error metadata, such as a status code, a general error title, and a list of
 * specific details. It is intended to be caught by a global GraphQL exception handler (like {@code
 * GraphQlExceptionHandler}) which can then use this structured information to build a detailed and
 * consistent error response for the client. This approach provides a richer error-handling
 * experience than a simple message string.
 */
public class CustomGraphQLException extends RuntimeException {

  /**
   * An HTTP-like status code (e.g., 400 for Bad Request, 404 for Not Found) to provide a familiar
   * error context to clients.
   */
  private final int statusCode;

  /** A short, human-readable error title (e.g., "Not Found", "Validation Error"). */
  private final String error;

  /**
   * A list of specific, detailed error messages. This is particularly useful for reporting multiple
   * validation failures at once.
   */
  private final List<String> details;

  /** The exact moment in time when the error occurred. */
  private final Instant timestamp;

  /**
   * Constructs a new CustomGraphQLException with detailed error information.
   *
   * @param statusCode The HTTP-like status code for the error.
   * @param error A short, descriptive error title.
   * @param message The main error message, passed to the superclass. This is typically what a
   *     client would display to an end-user.
   * @param path The GraphQL path where the error occurred (often unused here as it's handled by the
   *     exception resolver).
   * @param details A list of specific, detailed error strings.
   */
  public CustomGraphQLException(
      int statusCode,
      String error,
      String message,
      String path, // Path is often captured by the resolver, but included for completeness.
      List<String> details) {
    super(message);
    this.statusCode = statusCode;
    this.error = error;
    this.details = details;
    this.timestamp = Instant.now();
  }

  /**
   * Gets the HTTP-like status code.
   *
   * @return The integer status code.
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Gets the short, human-readable error title.
   *
   * @return The error title string.
   */
  public String getError() {
    return error;
  }

  /**
   * Gets the list of detailed error messages.
   *
   * @return A list of strings containing error details.
   */
  public List<String> getDetails() {
    return details;
  }

  /**
   * Gets the timestamp of when the error occurred.
   *
   * @return The {@link Instant} the exception was created.
   */
  public Instant getTimestamp() {
    return timestamp;
  }
}
