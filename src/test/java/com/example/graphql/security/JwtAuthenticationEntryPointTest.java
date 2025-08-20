package com.example.graphql.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.graphql.api.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationEntryPointTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private AuthenticationException authException;

    private JwtAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Test
    void testCommence() throws IOException {
        // Arrange
        String path = "/test/path";
        byte[] errorBytes = "{\"message\":\"Unauthorized\"}".getBytes();
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.RequestPath requestPath =
                mock(org.springframework.http.server.RequestPath.class);
        org.springframework.http.server.reactive.ServerHttpResponse response =
                mock(org.springframework.http.server.reactive.ServerHttpResponse.class);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        org.springframework.core.io.buffer.DataBufferFactory bufferFactory =
                mock(org.springframework.core.io.buffer.DataBufferFactory.class);
        org.springframework.core.io.buffer.DataBuffer dataBuffer =
                mock(org.springframework.core.io.buffer.DataBuffer.class);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(path);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(null);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(objectMapper.writeValueAsBytes(any(ApiError.class))).thenReturn(errorBytes);
        when(bufferFactory.wrap(errorBytes)).thenReturn(dataBuffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(entryPoint.commence(exchange, authException)).verifyComplete();

        // Verify interactions
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(objectMapper).writeValueAsBytes(any(ApiError.class));
        verify(response).writeWith(any());
    }

    @Test
    void testCommenceWithMapperException() throws IOException {
        // Arrange
        String path = "/test/path";
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.RequestPath requestPath =
                mock(org.springframework.http.server.RequestPath.class);
        org.springframework.http.server.reactive.ServerHttpResponse response =
                mock(org.springframework.http.server.reactive.ServerHttpResponse.class);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(path);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(null);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.getHeaders()).thenReturn(headers);
        when(objectMapper.writeValueAsBytes(any(ApiError.class))).thenThrow(new RuntimeException("Mapper error"));

        // Act & Assert
        StepVerifier.create(entryPoint.commence(exchange, authException))
                .expectErrorMatches(
                        ex -> ex instanceof RuntimeException && ex.getMessage().contains("Mapper error"))
                .verify();
    }

    @Test
    void testApiErrorCreation() throws IOException {
        // Arrange
        String path = "/test/path";
        byte[] errorBytes = "{\"message\":\"Unauthorized\"}".getBytes();
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.RequestPath requestPath =
                mock(org.springframework.http.server.RequestPath.class);
        org.springframework.http.server.reactive.ServerHttpResponse response =
                mock(org.springframework.http.server.reactive.ServerHttpResponse.class);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        org.springframework.core.io.buffer.DataBufferFactory bufferFactory =
                mock(org.springframework.core.io.buffer.DataBufferFactory.class);
        org.springframework.core.io.buffer.DataBuffer dataBuffer =
                mock(org.springframework.core.io.buffer.DataBuffer.class);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(path);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getStatusCode()).thenReturn(null);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.getHeaders()).thenReturn(headers);
        when(response.bufferFactory()).thenReturn(bufferFactory);
        when(objectMapper.writeValueAsBytes(any(ApiError.class))).thenReturn(errorBytes);
        when(bufferFactory.wrap(errorBytes)).thenReturn(dataBuffer);
        when(response.writeWith(any())).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = entryPoint.commence(exchange, authException);

        // Assert
        StepVerifier.create(result).verifyComplete();

        // Verify ApiError creation
        verify(objectMapper).writeValueAsBytes(argThat(apiError -> {
            assertTrue(apiError instanceof ApiError);
            ApiError error = (ApiError) apiError;
            assertEquals(HttpStatus.UNAUTHORIZED.value(), error.getStatus());
            assertEquals("Unauthorized", error.getMessage());
            assertEquals("Authentication is required to access this resource.", error.getDetails());
            assertEquals(path, error.getPath());
            assertNotNull(error.getTimestamp());
            return true;
        }));
    }
}
