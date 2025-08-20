package com.example.graphql.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class JwtWebFilterTest {

    @Mock
    private ReactiveUserDetailsService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private WebFilterChain chain;

    private JwtWebFilter jwtWebFilter;

    @BeforeEach
    void setUp() {
        jwtWebFilter = new JwtWebFilter(userService, jwtUtil);
    }

    @Test
    void testPublicEndpointSkipped() {
        // Arrange
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        when(exchange.getRequest()).thenReturn(request);
        URI uri = URI.create("/graphql/public/test");
        when(request.getURI()).thenReturn(uri);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoMoreInteractions(userService, jwtUtil);
    }

    @Test
    void testNoAuthorizationHeader() {
        // Arrange
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.HttpHeaders headers = mock(org.springframework.http.HttpHeaders.class);
        when(exchange.getRequest()).thenReturn(request);
        URI uri = URI.create("/graphql/protected");
        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn(null);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoMoreInteractions(userService, jwtUtil);
    }

    @Test
    void testValidJwtToken() {
        // Arrange
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.HttpHeaders headers = mock(org.springframework.http.HttpHeaders.class);
        when(exchange.getRequest()).thenReturn(request);
        URI uri = URI.create("/graphql/protected");
        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        String token = "validToken";
        String username = "testuser";
        UserDetails userDetails = User.withUsername(username)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        when(headers.getFirst("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Mono.just(userDetails));
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, chain)).verifyComplete();

        verify(userService).findByUsername(username);
        verify(jwtUtil).extractUsername(token);
        verify(jwtUtil).validateToken(token, userDetails);
        verify(chain, times(2)).filter(any(ServerWebExchange.class));
    }

    @Test
    void testInvalidJwtToken() {
        // Arrange
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.HttpHeaders headers = mock(org.springframework.http.HttpHeaders.class);
        when(exchange.getRequest()).thenReturn(request);
        URI uri = URI.create("/graphql/protected");
        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        String token = "invalidToken";
        String username = "testuser";
        UserDetails userDetails = User.withUsername(username)
                .password("password")
                .authorities(Collections.emptyList())
                .build();
        when(headers.getFirst("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Mono.just(userDetails));
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, chain)).verifyComplete();

        verify(userService).findByUsername(username);
        verify(jwtUtil).extractUsername(token);
        verify(jwtUtil).validateToken(token, userDetails);
        verify(chain, times(2)).filter(exchange);
    }

    @Test
    void testUserNotFound() {
        // Arrange
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.HttpHeaders headers = mock(org.springframework.http.HttpHeaders.class);
        when(exchange.getRequest()).thenReturn(request);
        URI uri = URI.create("/graphql/protected");
        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(headers);
        String token = "someToken";
        String username = "testuser";
        when(headers.getFirst("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(jwtWebFilter.filter(exchange, chain)).verifyComplete();

        verify(userService).findByUsername(username);
        verify(jwtUtil).extractUsername(token);
        verify(chain).filter(exchange);
    }
}
