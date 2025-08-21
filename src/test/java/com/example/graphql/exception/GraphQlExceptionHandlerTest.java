package com.example.graphql.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
public class GraphQlExceptionHandlerTest {

    private GraphQlExceptionHandler exceptionHandler;

    @Mock
    private DataFetchingEnvironment mockEnv;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GraphQlExceptionHandler();
        // No global stubbing needed; each test uses its own mockEnvWithFieldAndStepInfo()
    }

    private DataFetchingEnvironment mockEnvWithField() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        Field field = mock(Field.class);
        SourceLocation sourceLocation = mock(SourceLocation.class);
        when(env.getField()).thenReturn(field);
        when(field.getSourceLocation()).thenReturn(sourceLocation);
        return env;
    }

    private DataFetchingEnvironment mockEnvWithFieldAndStepInfo() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        Field field = mock(Field.class);
        SourceLocation sourceLocation = mock(SourceLocation.class);
        ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
        ResultPath resultPath = ResultPath.fromList(java.util.Collections.singletonList("testPath"));
        when(env.getField()).thenReturn(field);
        when(field.getSourceLocation()).thenReturn(sourceLocation);
        when(env.getExecutionStepInfo()).thenReturn(stepInfo);
        when(stepInfo.getPath()).thenReturn(resultPath);
        return env;
    }

    @Test
    void testCustomGraphQLException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        CustomGraphQLException customEx =
                new CustomGraphQLException(400, "TEST_ERROR", "Test error", "Test details", List.of("Some detail"));

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(customEx, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.BAD_REQUEST, graphQLError.getErrorType());
        assertEquals("Test error", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("CUSTOM_ERROR", extensions.get("code"));
        assertEquals(400, extensions.get("statusCode"));
        assertEquals("TEST_ERROR", extensions.get("error"));
        assertEquals("Test details", extensions.get("details"));
    }

    @Test
    void testConstraintViolationException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        ConstraintViolation<?> mockViolation = mock(ConstraintViolation.class);
        when(mockViolation.getPropertyPath()).thenReturn(mock());
        when(mockViolation.getPropertyPath().toString()).thenReturn("testField");
        when(mockViolation.getMessage()).thenReturn("Invalid value");

        Set<ConstraintViolation<?>> violations = Set.of(mockViolation);
        ConstraintViolationException violationEx = new ConstraintViolationException(violations);

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(violationEx, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.BAD_REQUEST, graphQLError.getErrorType());
        assertEquals("Validation failed", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("VALIDATION_ERROR", extensions.get("code"));

        List<String> validationErrors = (List<String>) extensions.get("validationErrors");
        assertNotNull(validationErrors);
        assertEquals(1, validationErrors.size());
        assertEquals("testField: Invalid value", validationErrors.get(0));
    }

    @Test
    void testMethodArgumentNotValidException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("objectName", "testField", "Invalid input");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException manve = mock(MethodArgumentNotValidException.class);
        when(manve.getBindingResult()).thenReturn(bindingResult);

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(manve, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.BAD_REQUEST, graphQLError.getErrorType());
        assertEquals("Input validation failed", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("ARGUMENT_NOT_VALID", extensions.get("code"));

        List<String> validationErrors = (List<String>) extensions.get("validationErrors");
        assertNotNull(validationErrors);
        assertEquals(1, validationErrors.size());
        assertEquals("testField: Invalid input", validationErrors.get(0));
    }

    @Test
    void testIllegalArgumentException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(ex, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.BAD_REQUEST, graphQLError.getErrorType());
        assertEquals("Invalid input provided", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("ILLEGAL_ARGUMENT", extensions.get("code"));
    }

    @Test
    void testDataIntegrityViolationException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Constraint violation");

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(ex, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.INTERNAL_ERROR, graphQLError.getErrorType());
        assertEquals("Database constraint violation", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("DATA_INTEGRITY_ERROR", extensions.get("code"));
    }

    @Test
    void testAccessDeniedException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(ex, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.FORBIDDEN, graphQLError.getErrorType());
        assertEquals("You do not have permission to perform this action", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("FORBIDDEN", extensions.get("code"));
    }

    @Test
    void testUnexpectedException() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        RuntimeException ex = new RuntimeException("Unexpected error");

        // Act
        GraphQLError graphQLError = exceptionHandler.resolveToSingleError(ex, env);

        // Assert
        assertNotNull(graphQLError);
        assertEquals(ErrorType.INTERNAL_ERROR, graphQLError.getErrorType());
        assertEquals("An unexpected error occurred. Please contact support.", graphQLError.getMessage());

        var extensions = graphQLError.getExtensions();
        assertNotNull(extensions);
        assertEquals("INTERNAL_ERROR", extensions.get("code"));
    }

    @Test
    void testTraceIdInExtensions() {
        DataFetchingEnvironment env = mockEnvWithFieldAndStepInfo();
        // Arrange
        try (MockedStatic<MDC> mdcMockedStatic = mockStatic(MDC.class)) {
            mdcMockedStatic.when(() -> MDC.get("traceId")).thenReturn("test-trace-id");

            RuntimeException ex = new RuntimeException("Unexpected error");

            // Act
            GraphQLError graphQLError = exceptionHandler.resolveToSingleError(ex, env);

            // Assert
            var extensions = graphQLError.getExtensions();
            assertNotNull(extensions);
            assertEquals("test-trace-id", extensions.get("traceId"));
        }
    }
}
