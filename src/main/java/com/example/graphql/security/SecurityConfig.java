package com.example.graphql.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Centralized security configuration for the application using Spring Security 6.
 *
 * <p>This class establishes the application's security posture. It configures a stateless,
 * token-based authentication system suitable for modern APIs. Key responsibilities include:
 *
 * <ul>
 *   <li>Defining the main security filter chain.
 *   <li>Configuring Cross-Origin Resource Sharing (CORS) to allow frontend clients.
 *   <li>Specifying which API endpoints are public and which require authentication.
 *   <li>Enabling method-level security with {@code @PreAuthorize} for fine-grained access control.
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtRequestFilter jwtRequestFilter;

  /**
   * Injects required security components using constructor injection.
   *
   * @param jwtAuthenticationEntryPoint Handles authentication errors (e.g., invalid token) by
   *     returning a 401 Unauthorized response.
   * @param jwtRequestFilter Intercepts requests to validate the JWT and set the authentication
   *     context. The {@code @Lazy} annotation is crucial here to break a circular dependency bean
   *     creation cycle: SecurityConfig -> JwtRequestFilter -> UserService -> PasswordEncoder (from
   *     SecurityConfig).
   */
  @Autowired
  public SecurityConfig(
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      @Lazy JwtRequestFilter jwtRequestFilter) {
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.jwtRequestFilter = jwtRequestFilter;
  }

  /**
   * Creates a bean for the password encoder.
   *
   * @return A {@link BCryptPasswordEncoder} instance. BCrypt is the industry standard for hashing
   *     passwords as it includes a salt automatically, protecting against rainbow table attacks.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Exposes the {@link AuthenticationManager} as a bean.
   *
   * <p>The AuthenticationManager is the core of Spring Security's authentication mechanism. It's
   * required by our custom authentication endpoint (e.g., {@code /auth/login}) to process user
   * credentials and authenticate them.
   *
   * @param config The authentication configuration provided by Spring.
   * @return The configured {@link AuthenticationManager}.
   * @throws Exception if an error occurs when retrieving the manager.
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Defines the primary security filter chain that governs all HTTP traffic.
   *
   * <p>This is the heart of the web security configuration. It uses a fluent API to chain together
   * various security rules and filters in a specific order.
   *
   * @param http The {@link HttpSecurity} object to configure.
   * @return The built {@link SecurityFilterChain}.
   * @throws Exception if an error occurs during configuration.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. Disable CSRF: Not needed for stateless APIs where authentication is done via tokens.
        // CSRF protection is primarily for stateful, session-based applications.
        .csrf(csrf -> csrf.disable())

        // 2. Configure CORS: Apply the custom CORS settings defined in the corsConfigurationSource
        // bean.
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 3. Define Authorization Rules: Specify access control for different URL patterns.
        .authorizeHttpRequests(
            authz ->
                authz
                    // Public Endpoints: Permit all requests to these paths without authentication.
                    // This includes login/register, the GraphQL endpoint itself, the GraphiQL UI,
                    // and monitoring endpoints. Fine-grained GraphQL security is handled at the
                    // method level with @PreAuthorize.
                    .requestMatchers(
                        "/auth/**", "/graphql/**", "/graphiql/**", "/public/**", "/actuator/**")
                    .permitAll()
                    // H2 Console: Permit access for development purposes.
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    // Role-Based Access: Restrict access to /admin/** endpoints to users with the
                    // 'ADMIN' role.
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    // Default Rule: All other requests not explicitly matched above must be
                    // authenticated.
                    .anyRequest()
                    .authenticated())

        // 4. Configure Exception Handling: Set the custom entry point for authentication failures.
        // If an unauthenticated user tries to access a protected resource, this entry point is
        // triggered.
        .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

        // 5. Configure Session Management: Enforce a stateless session policy.
        // The server will not create or use any HttpSession, making the API truly stateless.
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 6. Configure Headers: Specifically to allow the H2 console to be rendered in a frame.
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    // 7. Add Custom Filter: Insert our JWT filter before the standard Spring Security filter.
    // This ensures we validate the token and set up the security context early in the request
    // lifecycle.
    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Defines the Cross-Origin Resource Sharing (CORS) configuration.
   *
   * <p>This is essential for allowing frontend applications (e.g., React, Angular, Vue) hosted on
   * different domains (like http://localhost:3000) to securely communicate with this backend API.
   *
   * @return A {@link CorsConfigurationSource} bean that applies CORS rules to all endpoints.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // Define allowed origins. Using `setAllowedOriginPatterns` is more flexible and secure
    // than `setAllowedOrigins`, especially when `allowCredentials` is true.
    // IMPORTANT: Replace "*.your-frontend-domain.com" with your actual production frontend domain.
    config.setAllowedOriginPatterns(
        List.of("http://localhost:3000", "https://*.your-frontend-domain.com"));

    // Specify which HTTP methods are allowed from cross-origin requests.
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Specify which HTTP headers the client is allowed to include in requests.
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

    // Allow credentials (like cookies, authorization headers with tokens) to be sent.
    config.setAllowCredentials(true);

    // Specify which headers can be exposed in the response back to the client.
    config.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // Apply this CORS configuration to all paths ("/**").
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
