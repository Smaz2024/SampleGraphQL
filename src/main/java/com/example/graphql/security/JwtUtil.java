package com.example.graphql.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * A utility component for handling JSON Web Token (JWT) operations.
 *
 * <p>This class is responsible for generating, parsing, and validating JWTs used for stateless
 * authentication in the application. It is configured via application properties for the secret key
 * and token expiration times.
 */
@Component
public class JwtUtil {

  private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

  /**
   * The secret key used for signing and verifying JWTs. This should be a long, complex, and
   * securely stored string, injected from application properties.
   */
  @Value("${jwt.secret}")
  private String secret;

  /** The expiration time for standard access tokens, in milliseconds. */
  @Value("${jwt.expiration}")
  private Long expiration;

  /**
   * The expiration time for refresh tokens, in milliseconds. This is typically much longer than the
   * access token expiration.
   */
  @Value("${jwt.refresh-expiration}")
  private Long refreshExpiration;

  /** The cryptographically secure key derived from the secret string. */
  private SecretKey secretKey;

  /** A reusable, thread-safe parser for validating and parsing JWTs. */
  private JwtParser jwtParser;

  /**
   * Initializes the component after dependency injection is complete. This method validates the
   * secret key's strength and prepares the {@link SecretKey} and {@link JwtParser} for use.
   */
  @PostConstruct
  public void init() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    // Security Best Practice: Ensure the secret key is strong enough for the HS256 algorithm.
    if (keyBytes.length < 32) {
      logger.error("FATAL: JWT secret must be at least 256 bits (32 bytes) long for HS256.");
      throw new IllegalArgumentException("JWT secret is too weak.");
    }
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);

    // Build a reusable parser. This is more efficient than creating one per call.
    this.jwtParser =
        Jwts.parser()
            .verifyWith(secretKey)
            // Tolerate minor clock differences between the server that issued the token and this
            // one.
            .setAllowedClockSkewSeconds(30)
            .build();
  }

  /**
   * Extracts the username (the 'subject' claim) from a JWT.
   *
   * @param token The JWT string.
   * @return The username.
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extracts the expiration date from a JWT.
   *
   * @param token The JWT string.
   * @return The expiration date.
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * A generic method to extract a specific claim from a JWT using a claims resolver function.
   *
   * @param token The JWT string.
   * @param resolver A function that takes a {@link Claims} object and returns the desired value.
   * @param <T> The type of the claim to be returned.
   * @return The extracted claim.
   */
  public <T> T extractClaim(String token, Function<Claims, T> resolver) {
    Claims claims = extractAllClaims(token);
    return resolver.apply(claims);
  }

  /**
   * Parses the JWT and returns all its claims. This method implicitly validates the token's
   * signature and expiration.
   *
   * @param token The JWT string.
   * @return The {@link Claims} object containing the token's payload.
   */
  private Claims extractAllClaims(String token) {
    return jwtParser.parseSignedClaims(token).getPayload();
  }

  /**
   * Generates a standard access token for a given user. Includes the user's roles as a custom
   * claim.
   *
   * @param userDetails The user details object from Spring Security.
   * @return A signed and compact JWT string.
   */
  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    List<String> roles =
        userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    claims.put("roles", roles);
    return createToken(claims, userDetails.getUsername(), expiration);
  }

  /**
   * Generates a long-lived refresh token for a given user. This token typically has no extra claims
   * and is used solely to obtain a new access token.
   *
   * @param userDetails The user details object from Spring Security.
   * @return A signed and compact JWT string.
   */
  public String generateRefreshToken(UserDetails userDetails) {
    return createToken(Collections.emptyMap(), userDetails.getUsername(), refreshExpiration);
  }

  /**
   * The core token creation logic.
   *
   * @param claims A map of custom claims to include in the payload.
   * @param subject The subject of the token (usually the username).
   * @param ttlMillis The time-to-live for the token in milliseconds.
   * @return A signed and compact JWT string.
   */
  private String createToken(Map<String, Object> claims, String subject, Long ttlMillis) {
    Date now = new Date();
    return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setId(
            UUID.randomUUID()
                .toString()) // jti (JWT ID) claim: provides a unique identifier for the token
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + ttlMillis))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Validates a token against a user's details. It checks if the token is valid (signature,
   * expiration) and if it belongs to the specified user.
   *
   * @param token The JWT string to validate.
   * @param userDetails The user to validate against.
   * @return {@code true} if the token is valid and belongs to the user, {@code false} otherwise.
   */
  public boolean validateToken(String token, UserDetails userDetails) {
    try {
      // The parser handles signature and expiration validation. We just check if the subject
      // matches.
      final String username = extractUsername(token);
      return username.equals(userDetails.getUsername());
    } catch (ExpiredJwtException eje) {
      logger.warn(
          "Expired JWT token for user '{}': {}", userDetails.getUsername(), eje.getMessage());
    } catch (JwtException | IllegalArgumentException ex) {
      logger.warn(
          "Invalid JWT token for user '{}': {}", userDetails.getUsername(), ex.getMessage());
    }
    return false;
  }

  /**
   * Performs a simple validation on the token, checking only its structural integrity, signature,
   * and expiration.
   *
   * @param token The JWT string to validate.
   * @return {@code true} if the token is valid, {@code false} otherwise.
   */
  public boolean validateToken(String token) {
    try {
      // If this call succeeds without throwing an exception, the token is valid.
      jwtParser.parseSignedClaims(token);
      return true;
    } catch (ExpiredJwtException eje) {
      logger.warn("Expired JWT token: {}", eje.getMessage());
    } catch (JwtException | IllegalArgumentException ex) {
      logger.warn("Invalid JWT token: {}", ex.getMessage());
    }
    return false;
  }
}
