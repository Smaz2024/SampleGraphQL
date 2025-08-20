// Requires: JUnit 5, Mockito (Jupiter), Reactor Test, Spring WebFlux

package com.example.graphql.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.graphql.entity.ExternalServiceResponse;
import com.example.graphql.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ExternalServiceClientTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    private ExternalServiceClient client;

    @BeforeEach
    void setUp() {
        // Default client instance backed by an ExchangeFunction stub for integration-like tests
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            String url = request.url().toString();
            if (url.contains("api.service-a.com")) {
                return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"data\":\"A:ok\"}")
                        .build());
            } else if (url.contains("api.service-b.com")) {
                return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"data\":\"B:ok\"}")
                        .build());
            } else if (url.contains("/external-api")) {
                return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .build());
            }
            return Mono.just(ClientResponse.create(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                    .body("unavailable")
                    .build());
        });

        client = new ExternalServiceClient(jwtUtil, builder);
        // userService is injected by Spring in production; in tests we mock interactions directly
    }

    // Helper: create a mock WebClient chain for GET requests that returns expected ExternalServiceResponse
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static WebClient mockWebClientChain(String expectedResponseData) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenAnswer(invocation -> {
            Class<?> clazz = invocation.getArgument(0);
            if (clazz == Void.class) {
                return Mono.empty();
            } else if (clazz == ExternalServiceResponse.class) {
                return Mono.just(new ExternalServiceResponse(expectedResponseData));
            } else {
                return Mono.error(new IllegalArgumentException("Unexpected type: " + clazz));
            }
        });

        return webClient;
    }

    @Test
    void callServiceA_validToken_returnsData() {
        String token = "valid.jwt.token";
        String username = "john";
        UserDetails details = mock(UserDetails.class);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.loadUserByUsername(username)).thenReturn(details);
        when(jwtUtil.validateToken(token, details)).thenReturn(true);

        // Use mock WebClient chain for predictable response mapping
        client = new ExternalServiceClient(jwtUtil, mockWebClientChain("A:ok"));

        Mono<ExternalServiceResponse> result = client.callServiceA("123", token);

        StepVerifier.create(result)
                .assertNext(resp -> assertThat(resp.getData()).isEqualTo("A:ok"))
                .verifyComplete();

        verify(jwtUtil).extractUsername(token);
        verify(userService).loadUserByUsername(username);
        verify(jwtUtil).validateToken(token, details);
    }

    @Test
    void callServiceB_validToken_returnsData() {
        String token = "valid.jwt.token";
        String username = "jane";
        UserDetails details = mock(UserDetails.class);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.loadUserByUsername(username)).thenReturn(details);
        when(jwtUtil.validateToken(token, details)).thenReturn(true);

        client = new ExternalServiceClient(jwtUtil, mockWebClientChain("B:ok"));

        Mono<ExternalServiceResponse> result = client.callServiceB("456", token);

        StepVerifier.create(result)
                .assertNext(resp -> assertThat(resp.getData()).isEqualTo("B:ok"))
                .verifyComplete();

        verify(jwtUtil).extractUsername(token);
        verify(userService).loadUserByUsername(username);
        verify(jwtUtil).validateToken(token, details);
    }

    @Test
    void callServiceA_missingToken_errorsWithIllegalArgument() {
        Mono<ExternalServiceResponse> result = client.callServiceA("id", " ");

        StepVerifier.create(result)
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().contains("Missing/empty token"))
                .verify();
    }

    @Test
    void callServiceA_invalidJwtParsing_errorsWithJwtException() {
        String token = "bad.jwt";
        when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("parse failure"));

        Mono<ExternalServiceResponse> result = client.callServiceA("id", token);

        StepVerifier.create(result).expectError(JwtException.class).verify();

        verify(jwtUtil).extractUsername(token);
        verifyNoInteractions(userService);
    }

    @Test
    void callServiceA_validationFails_errorsWithJwtException() {
        String token = "valid.jwt";
        String username = "john";
        UserDetails details = mock(UserDetails.class);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.loadUserByUsername(username)).thenReturn(details);
        when(jwtUtil.validateToken(token, details)).thenReturn(false);

        Mono<ExternalServiceResponse> result = client.callServiceA("id", token);

        StepVerifier.create(result).expectError(JwtException.class).verify();

        verify(jwtUtil).extractUsername(token);
        verify(userService).loadUserByUsername(username);
        verify(jwtUtil).validateToken(token, details);
    }

    @Test
    void callExternalService_validToken_completes() {
        String token = "valid.jwt.token";
        String username = "jack";
        com.example.graphql.entity.User user = new com.example.graphql.entity.User(
                username, "jack@example.com", "password", com.example.graphql.entity.User.Role.USER);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.findByUsernameReactive(username)).thenReturn(Mono.just(user));
        when(jwtUtil.validateToken(token, user)).thenReturn(true);

        client = new ExternalServiceClient(jwtUtil, mockWebClientChain("ok"));

        StepVerifier.create(client.callExternalService(token)).verifyComplete();

        verify(jwtUtil).extractUsername(token);
        verify(userService).findByUsernameReactive(username);
        verify(jwtUtil).validateToken(token, user);
    }

    @Test
    void callExternalService_missingToken_errors() {
        StepVerifier.create(client.callExternalService(""))
                .expectErrorMatches(
                        ex -> ex instanceof RuntimeException && ex.getMessage().contains("JWT token missing"))
                .verify();
    }

    @Test
    void callExternalService_invalidTokenParsing_errors() {
        String token = "bad";
        when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("parse error"));

        StepVerifier.create(client.callExternalService(token))
                .expectErrorMatches(
                        ex -> ex instanceof RuntimeException && ex.getMessage().contains("Invalid JWT token"))
                .verify();

        verify(jwtUtil).extractUsername(token);
        verifyNoInteractions(userService);
    }

    @Test
    void callExternalService_validationFails_errors() {
        String token = "valid.jwt";
        String username = "amy";
        com.example.graphql.entity.User user = new com.example.graphql.entity.User(
                username, "amy@example.com", "password", com.example.graphql.entity.User.Role.USER);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.findByUsernameReactive(username)).thenReturn(Mono.just(user));
        when(jwtUtil.validateToken(token, user)).thenReturn(false);

        StepVerifier.create(client.callExternalService(token))
                .expectErrorMatches(
                        ex -> ex instanceof RuntimeException && ex.getMessage().contains("JWT token validation failed"))
                .verify();

        verify(jwtUtil).extractUsername(token);
        verify(userService).findByUsernameReactive(username);
        verify(jwtUtil).validateToken(token, user);
    }

    @Test
    void testCallExternalService_Success() {
        String token = "valid.jwt.token";
        String username = "alice";
        com.example.graphql.entity.User user = new com.example.graphql.entity.User(
                username, "alice@example.com", "password", com.example.graphql.entity.User.Role.USER);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.findByUsernameReactive(username)).thenReturn(Mono.just(user));
        when(jwtUtil.validateToken(token, user)).thenReturn(true);

        client = new ExternalServiceClient(jwtUtil, mockWebClientChain("ok"));

        StepVerifier.create(client.callExternalService(token)).verifyComplete();

        verify(jwtUtil).extractUsername(token);
        verify(userService).findByUsernameReactive(username);
        verify(jwtUtil).validateToken(token, user);
    }

    @Test
    void testCallServiceA_TokenValidationFailed() {
        String token = "valid.jwt.token";
        String username = "bob";
        UserDetails details = mock(UserDetails.class);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.loadUserByUsername(username)).thenReturn(details);
        when(jwtUtil.validateToken(token, details)).thenReturn(false);

        client = new ExternalServiceClient(jwtUtil, mockWebClientChain("A:ok"));

        Mono<ExternalServiceResponse> result = client.callServiceA("123", token);

        StepVerifier.create(result).expectError(JwtException.class).verify();

        verify(jwtUtil).extractUsername(token);
        verify(userService).loadUserByUsername(username);
        verify(jwtUtil).validateToken(token, details);
    }
}
