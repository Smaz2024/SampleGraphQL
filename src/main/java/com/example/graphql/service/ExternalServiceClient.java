package com.example.graphql.service;

import com.example.graphql.entity.ExternalServiceResponse;
import com.example.graphql.security.JwtUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * A resilient client for interacting with external microservices.
 *
 * <p>This service encapsulates the logic for calling downstream APIs, incorporating resilience
 * patterns like Circuit Breakers and Rate Limiters from Resilience4j. It also handles passing along
 * authentication tokens.
 */
@Service
public class ExternalServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(ExternalServiceClient.class);

  /**
   * A logical name for the backend service configuration in Resilience4j. This name is used in
   * application.yml to configure the circuit breaker and rate limiter properties.
   */
  private static final String BACKEND = "externalService";

  private final WebClient webClient;
  private final JwtUtil jwtUtil;

  /**
   * Constructs the client with a pre-configured WebClient builder and JWT utility.
   *
   * @param jwtUtil The utility for handling JWT operations.
   * @param webClientBuilder The builder used to create a WebClient instance. Using the builder
   *     allows for easy customization (e.g., setting timeouts, codecs).
   */
  @Autowired
  public ExternalServiceClient(JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
    this.jwtUtil = jwtUtil;
    this.webClient = webClientBuilder.build();
  }

  /**
   * Calls an external service 'A' to fetch data. The call is protected by a circuit breaker and a
   * rate limiter.
   *
   * @param id The resource identifier to fetch.
   * @param token The JWT for authenticating with the downstream service.
   * @return A {@link Mono} emitting the response from Service A, or a fallback response if the
   *     service is unavailable.
   */
  @CircuitBreaker(name = BACKEND, fallbackMethod = "fallbackServiceA")
  @RateLimiter(name = BACKEND)
  public Mono<ExternalServiceResponse> callServiceA(String id, String token) {
    // The reactive chain first validates the token. If validation fails, the chain is terminated
    // with an error.
    // If successful, it proceeds to make the WebClient call.
    return validateTokenMono(token)
        .then(
            webClient
                .get()
                .uri("https://api.service-a.com/data/{id}", id)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ExternalServiceResponse.class));
  }

  /**
   * Calls an external service 'B' to fetch data. This call is protected by the same resilience
   * policies as Service A.
   *
   * @param id The resource identifier to fetch.
   * @param token The JWT for authenticating with the downstream service.
   * @return A {@link Mono} emitting the response from Service B, or a fallback response if the
   *     service is unavailable.
   */
  @CircuitBreaker(name = BACKEND, fallbackMethod = "fallbackServiceB")
  @RateLimiter(name = BACKEND)
  public Mono<ExternalServiceResponse> callServiceB(String id, String token) {
    return validateTokenMono(token)
        .then(
            webClient
                .get()
                .uri("https://api.service-b.com/data/{id}", id)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ExternalServiceResponse.class));
  }

  /**
   * Fallback method for {@code callServiceA}, executed when the circuit breaker is open. The method
   * signature must match the original method, with an added {@link Throwable} parameter.
   *
   * @param id The original resource identifier.
   * @param token The original token.
   * @param ex The exception that triggered the fallback (e.g., CallNotPermittedException).
   * @return A {@link Mono} emitting a default/cached response.
   */
  private Mono<ExternalServiceResponse> fallbackServiceA(String id, String token, Throwable ex) {
    // Logging the exception is crucial for monitoring and alerting on service failures.
    logger.warn("Fallback for callServiceA(id={}) triggered. Reason: {}", id, ex.getMessage());
    return Mono.just(
        new ExternalServiceResponse("Service A is currently unavailable. Please try again later."));
  }

  /**
   * Fallback method for {@code callServiceB}.
   *
   * @param id The original resource identifier.
   * @param token The original token.
   * @param ex The exception that triggered the fallback.
   * @return A {@link Mono} emitting a default/cached response.
   */
  private Mono<ExternalServiceResponse> fallbackServiceB(String id, String token, Throwable ex) {
    logger.warn("Fallback for callServiceB(id={}) triggered. Reason: {}", id, ex.getMessage());
    return Mono.just(
        new ExternalServiceResponse("Service B is currently unavailable. Please try again later."));
  }

  /**
   * Validates a JWT within a reactive stream. It returns an empty Mono on success, or a Mono with
   * an error if the token is null, blank, or invalid.
   *
   * @param token The JWT string to validate.
   * @return A {@link Mono<Void>} that completes successfully or signals an error.
   */
  private Mono<Void> validateTokenMono(String token) {
    // Using Mono.defer ensures the validation logic is executed lazily for each subscription.
    return Mono.defer(
        () -> {
          if (token == null || token.isBlank()) {
            return Mono.error(
                new IllegalArgumentException("Authorization token is missing or empty."));
          }
          // The jwtUtil.validateToken method handles its own logging for specific failures (e.g.,
          // expiration).
          if (!jwtUtil.validateToken(token)) {
            return Mono.error(new JwtException("The provided JWT is invalid or expired."));
          }
          // An empty Mono signals successful completion of this validation step.
          return Mono.empty();
        });
  }
}
