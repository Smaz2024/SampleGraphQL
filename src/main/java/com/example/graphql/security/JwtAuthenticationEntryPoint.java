package com.example.graphql.security;

import com.example.graphql.api.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * A custom {@link AuthenticationEntryPoint} for handling authentication failures in a stateless
 * REST/GraphQL API.
 *
 * <p>This component is triggered by Spring Security's filter chain when an unauthenticated user
 * attempts to access a protected resource. Instead of redirecting to a login page (which is the
 * default behavior for stateful web applications), it returns a clear, structured JSON error
 * response with an HTTP 401 Unauthorized status code. This is the standard practice for APIs.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  /**
   * A Jackson JSON processor used to serialize the ApiError object into a JSON string. It's
   * injected via the constructor for better testability.
   */
  private final ObjectMapper mapper;

  /**
   * Constructs the entry point with a required ObjectMapper.
   *
   * @param mapper The Spring-managed ObjectMapper for JSON serialization.
   */
  @Autowired
  public JwtAuthenticationEntryPoint(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * This method is invoked whenever an unauthenticated user tries to access a secured endpoint.
   *
   * @param request The request that resulted in an AuthenticationException.
   * @param response The response, which will be modified to contain the 401 error.
   * @param authException The exception that triggered the commencement.
   * @throws IOException If an input or output exception occurs.
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    // Set the response content type to indicate that we are sending JSON data.
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    // Set the HTTP status code to 401 Unauthorized.
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    // Create a structured error object that provides consistent error information to the client.
    ApiError error =
        new ApiError(
            HttpServletResponse.SC_UNAUTHORIZED,
            "Unauthorized",
            authException
                .getMessage(), // The message from the security exception (e.g., "Bad credentials").
            request.getServletPath(),
            List.of() // This could be populated with more specific details if needed.
            );

    // Use the ObjectMapper to write the serialized ApiError object to the response's output stream.
    mapper.writeValue(response.getOutputStream(), error);
  }
}
