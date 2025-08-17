package com.example.graphql.security;

import com.example.graphql.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A custom Spring Security filter that intercepts incoming HTTP requests to validate JWTs.
 *
 * <p>This filter executes once per request. It checks for a JWT in the 'Authorization' header,
 * validates it, and if the token is valid, it sets up the Spring Security context with the user's
 * authentication details. This allows subsequent security mechanisms (like @PreAuthorize) to work
 * correctly.
 */
@Component
@Lazy
public class JwtRequestFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
  public static final String BEARER_PREFIX = "Bearer ";

  private final UserService userService;
  private final JwtUtil jwtUtil;

  @Autowired
  public JwtRequestFilter(UserService userService, JwtUtil jwtUtil) {
    this.userService = userService;
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/auth/") || path.startsWith("/graphql/public");
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    final String authHeader = request.getHeader("Authorization");
    String username = null;
    String jwtToken = null;

    if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
      jwtToken = authHeader.substring(BEARER_PREFIX.length());
      try {
        username = jwtUtil.extractUsername(jwtToken);
      } catch (IllegalArgumentException ex) {
        logger.warn("Unable to parse JWT token: {}", ex.getMessage());
      } catch (ExpiredJwtException ex) {
        logger.warn(
            "JWT token for user '{}' expired at {}: {}",
            ex.getClaims().getSubject(),
            ex.getClaims().getExpiration(),
            ex.getMessage());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token has expired");
        return;
      }
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // Our UserService returns a User object, which implements UserDetails.
      UserDetails userDetails = userService.loadUserByUsername(username);

      if (jwtUtil.validateToken(jwtToken, userDetails)) {
        // By setting the full User object as the principal, we avoid extra DB lookups later.
        // We also store the token as the credentials, making it available for downstream services.
        var authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, jwtToken, userDetails.getAuthorities());

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }
}
