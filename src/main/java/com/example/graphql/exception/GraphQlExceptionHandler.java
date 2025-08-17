package com.example.graphql.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GraphQlExceptionHandler.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        // Add correlation/trace ID if available (via Sleuth/OTel/MDC)
        String traceId = MDC.get("traceId");
        Map<String, Object> extensions = new LinkedHashMap<>();
        if (traceId != null) {
            extensions.put("traceId", traceId);
        }

        if (ex instanceof CustomGraphQLException customEx) {
            logger.error(
                    "CustomGraphQLException at path {}: {}",
                    env.getExecutionStepInfo().getPath(),
                    customEx.getMessage(),
                    ex);
            extensions.put("code", "CUSTOM_ERROR");
            extensions.put("statusCode", customEx.getStatusCode());
            extensions.put("error", customEx.getError());
            extensions.put("details", customEx.getDetails());
            extensions.put("timestamp", customEx.getTimestamp().toString());

            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(customEx.getMessage())
                    .extensions(extensions)
                    .build();
        }

        if (ex instanceof ConstraintViolationException violationEx) {
            logger.warn(
                    "Validation error at path {}: {}",
                    env.getExecutionStepInfo().getPath(),
                    violationEx.getMessage());
            extensions.put("code", "VALIDATION_ERROR");
            extensions.put(
                    "validationErrors",
                    violationEx.getConstraintViolations().stream()
                            .map(this::formatViolation)
                            .collect(Collectors.toList()));

            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message("Validation failed")
                    .extensions(extensions)
                    .build();
        }

        if (ex instanceof MethodArgumentNotValidException manve) {
            logger.warn("Method argument validation failed: {}", manve.getMessage());
            extensions.put("code", "ARGUMENT_NOT_VALID");
            extensions.put(
                    "validationErrors",
                    manve.getBindingResult().getFieldErrors().stream()
                            .map(this::formatFieldError)
                            .collect(Collectors.toList()));

            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message("Input validation failed")
                    .extensions(extensions)
                    .build();
        }

        if (ex instanceof IllegalArgumentException) {
            logger.warn(
                    "Illegal argument at path {}: {}",
                    env.getExecutionStepInfo().getPath(),
                    ex.getMessage());
            extensions.put("code", "ILLEGAL_ARGUMENT");

            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message("Invalid input provided")
                    .extensions(extensions)
                    .build();
        }

        if (ex instanceof DataIntegrityViolationException) {
            logger.error("Database integrity violation: {}", ex.getMessage());
            extensions.put("code", "DATA_INTEGRITY_ERROR");

            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .message("Database constraint violation")
                    .extensions(extensions)
                    .build();
        }

        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            logger.warn(
                    "Access denied at path {}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage());
            extensions.put("code", "FORBIDDEN");

            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.FORBIDDEN)
                    .message("You do not have permission to perform this action")
                    .extensions(extensions)
                    .build();
        }

        // Generic catch-all
        logger.error(
                "Unexpected error at path {}: {}", env.getExecutionStepInfo().getPath(), ex.getMessage(), ex);
        extensions.put("code", "INTERNAL_ERROR");

        return GraphqlErrorBuilder.newError(env)
                .errorType(ErrorType.INTERNAL_ERROR)
                .message("An unexpected error occurred. Please contact support.")
                .extensions(extensions)
                .build();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
