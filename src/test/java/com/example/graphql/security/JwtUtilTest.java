package com.example.graphql.security;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        userDetails = User.withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    void testGenerateToken() {
        // Act
        String token = jwtUtil.generateToken(userDetails);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        String extractedUsername = jwtUtil.extractUsername(token);

        // Assert
        assertEquals("testuser", extractedUsername);
    }

    @Test
    void testExtractClaim() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        Date issuedAt = jwtUtil.extractClaim(token, Claims::getIssuedAt);
        Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);

        // Assert
        assertNotNull(issuedAt);
        assertNotNull(expiration);
        assertTrue(expiration.after(issuedAt));
    }

    @Test
    void testValidateToken() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateTokenWithWrongUsername() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);
        UserDetails differentUser = User.withUsername("differentuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // Act
        boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void testTokenExpiration() throws InterruptedException {
        // Create a custom JwtUtil with very short expiration for testing
        JwtUtil shortExpirationJwtUtil = new JwtUtil() {
            @Override
            public String generateToken(UserDetails userDetails) {
                return Jwts.builder()
                        .subject(userDetails.getUsername())
                        .issuedAt(new Date(System.currentTimeMillis()))
                        .expiration(new Date(System.currentTimeMillis() + 100)) // 100ms expiration
                        .signWith(Keys.hmacShaKeyFor("your-32-byte-secret-key-should-be-long-enough!".getBytes()))
                        .compact();
            }
        };

        // Arrange
        String token = shortExpirationJwtUtil.generateToken(userDetails);

        // Wait to ensure token expires
        Thread.sleep(200);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            shortExpirationJwtUtil.validateToken(token, userDetails);
        });
    }
}
