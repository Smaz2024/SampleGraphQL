package com.example.graphql.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * A central component for handling exceptions that occur during GraphQL data fetching.
 *
 * <p>This class extends {@link DataFetcherExceptionResolverAdapter}, the standard Spring for
 * GraphQL mechanism for custom exception handling. It intercepts exceptions thrown
 * from @QueryMapping and @MutationMapping methods, translating them into structured {@link
 * GraphQLError} objects that are sent to the client. This ensures consistent and informative error
 * responses across the API.
 */
@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

  private static final Logger logger = LoggerFactory.getLogger(GraphQlExceptionHandler.class);

  /**
   * Resolves a thrown exception into a single, structured {@link GraphQLError}. This method is
   * automatically invoked by the GraphQL engine when a data fetcher throws an exception.
   *
   * @param ex The exception that was thrown.
   * @param env The environment of the data fetcher that threw the exception, providing context like
   *     the query path.
   * @return A structured {@link GraphQLError} to be included in the GraphQL response.
   */
  @Override
  protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
    String path = env.getExecutionStepInfo().getPath().toString();
    logger.error("Exception at '{}': {}", path, ex.getMessage(), ex);

    Map<String, Object> extensions =
        Map.of(
            "timestamp", Instant.now().toString(),
            "exception", ex.getClass().getSimpleName());

    if (ex instanceof AccessDeniedException) {
      return GraphqlErrorBuilder.newError(env)
          .errorType(ErrorType.FORBIDDEN)
          .message("Access denied: You do not have the required permissions for this operation.")
          .extensions(extensions)
          .build();
    }

    if (ex instanceof ConstraintViolationException cve) {
      List<Map<String, String>> validationErrors =
          cve.getConstraintViolations().stream()
              .map(
                  cv ->
                      Map.of(
                          "field", cv.getPropertyPath().toString(),
                          "message", cv.getMessage()))
              .sorted(Comparator.comparing(m -> m.get("field")))
              .collect(Collectors.toList());

      Map<String, Object> newExtensions = new HashMap<>(extensions);
      newExtensions.put("validationErrors", validationErrors);

      return GraphqlErrorBuilder.newError(env)
          .errorType(ErrorType.BAD_REQUEST)
          .message("Validation failed. See the 'extensions' field for details.")
          .extensions(newExtensions)
          .build();
    }

    if (ex instanceof CustomGraphQLException cge) {
      ErrorType type =
          switch (cge.getStatusCode()) {
            case 400 -> ErrorType.BAD_REQUEST;
            case 404 -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
          };
      return GraphqlErrorBuilder.newError(env)
          .errorType(type)
          .message(cge.getMessage())
          .extensions(extensions)
          .build();
    }

    return GraphqlErrorBuilder.newError(env)
        .errorType(ErrorType.INTERNAL_ERROR)
        .message("An internal server error occurred. Please contact support.")
        .extensions(extensions)
        .build();
  }
}
