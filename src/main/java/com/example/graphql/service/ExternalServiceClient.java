/**
 * Service for calling external APIs and aggregating their responses.
 */
package com.example.graphql.service;

import com.example.graphql.entity.ExternalServiceResponse;
import com.example.graphql.security.JwtUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ExternalServiceClient {
    // For testability: allow direct WebClient injection
    public ExternalServiceClient(JwtUtil jwtUtil, WebClient webClient) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceClient.class);
    private static final String BACKEND = "externalService";

    private final WebClient webClient;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    public ExternalServiceClient(JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
    }

    @CircuitBreaker(name = BACKEND, fallbackMethod = "fallbackServiceA")
    @RateLimiter(name = BACKEND)
    public Mono<ExternalServiceResponse> callServiceA(String id, String token) {
        return validateTokenMono(token)
                .then(webClient
                        .get()
                        .uri("https://api.service-a.com/data/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                                .map(RuntimeException::new))
                        .bodyToMono(ExternalServiceResponse.class));
    }

    @CircuitBreaker(name = BACKEND, fallbackMethod = "fallbackServiceB")
    @RateLimiter(name = BACKEND)
    public Mono<ExternalServiceResponse> callServiceB(String id, String token) {
        return validateTokenMono(token)
                .then(webClient
                        .get()
                        .uri("https://api.service-b.com/data/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                                .map(RuntimeException::new))
                        .bodyToMono(ExternalServiceResponse.class));
    }

    private Mono<ExternalServiceResponse> fallbackServiceA(String id, String token, Throwable ex) {
        logger.warn("Fallback for callServiceA(id={}) triggered. Reason: {}", id, ex.getMessage());
        return Mono.just(new ExternalServiceResponse("Service A unavailable. Try again later."));
    }

    private Mono<ExternalServiceResponse> fallbackServiceB(String id, String token, Throwable ex) {
        logger.warn("Fallback for callServiceB(id={}) triggered. Reason: {}", id, ex.getMessage());
        return Mono.just(new ExternalServiceResponse("Service B unavailable. Try again later."));
    }

    private Mono<Void> validateTokenMono(String token) {
        return Mono.defer(() -> {
            if (token == null || token.isBlank()) {
                return Mono.error(new IllegalArgumentException("Missing/empty token"));
            }
            String username;
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception ex) {
                return Mono.error(new JwtException("Invalid JWT token", ex));
            }
            UserDetails userDetails = userService.loadUserByUsername(username);
            if (!jwtUtil.validateToken(token, userDetails)) {
                return Mono.error(new JwtException("Invalid or expired JWT"));
            }
            return Mono.<Void>empty();
        });
    }

    public Mono<Void> callExternalService(String token) {
        if (token == null || token.isEmpty()) {
            return Mono.error(new RuntimeException("JWT token missing"));
        }

        String username;
        try {
            username = jwtUtil.extractUsername(token);
        } catch (Exception ex) {
            return Mono.error(new RuntimeException("Invalid JWT token", ex));
        }

        return userService.findByUsernameReactive(username).flatMap(userDetails -> {
            if (!jwtUtil.validateToken(token, userDetails)) {
                return Mono.error(new RuntimeException("JWT token validation failed"));
            }
            // Token valid, continue with external call
            return performExternalCall(token, userDetails);
        });
    }

    // Example reactive external call
    private Mono<Void> performExternalCall(String token, UserDetails userDetails) {
        return webClient
                .get()
                .uri("/external-api")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
