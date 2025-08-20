package com.example.graphql.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@ExtendWith(MockitoExtension.class)
public class SecurityConfigTest {

    @Mock
    private JwtWebFilter jwtWebFilter;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtWebFilter);
    }

    @Test
    void testPasswordEncoder() {
        // Act
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        // Assert
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder.getClass().getName().contains("BCryptPasswordEncoder"));
    }

    @Test
    void testCorsConfigurationSource() {
        // Act
        CorsConfigurationSource corsConfigSource = securityConfig.corsConfigurationSource();

        // Assert
        assertNotNull(corsConfigSource);

        // Verify CORS configuration
        org.springframework.web.server.ServerWebExchange exchange =
                mock(org.springframework.web.server.ServerWebExchange.class);
        org.springframework.http.server.reactive.ServerHttpRequest request =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.RequestPath path = mock(org.springframework.http.server.RequestPath.class);
        when(request.getPath()).thenReturn(path);
        when(exchange.getRequest()).thenReturn(request);
        CorsConfiguration config = corsConfigSource.getCorsConfiguration(exchange);
        assertNotNull(config);

        // Check allowed origins
        assertNotNull(config.getAllowedOriginPatterns());
        assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOriginPatterns().contains("https://*.your-frontend-domain.com"));

        // Check allowed methods
        assertNotNull(config.getAllowedMethods());
        assertTrue(
                config.getAllowedMethods().containsAll(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")));

        // Check allowed headers
        assertNotNull(config.getAllowedHeaders());
        assertTrue(config.getAllowedHeaders().contains("Authorization"));
        assertTrue(config.getAllowedHeaders().contains("Content-Type"));

        assertNotNull(config.getAllowCredentials());
        assertTrue(config.getAllowCredentials());

        assertNotNull(config.getExposedHeaders());
        assertTrue(config.getExposedHeaders().contains("Authorization"));
    }
}
