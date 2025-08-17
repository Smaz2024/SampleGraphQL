package com.example.graphql.controller;

import com.example.graphql.dto.UserUpdateDTO;
import com.example.graphql.entity.*;
import com.example.graphql.service.*;
import com.example.graphql.service.ExternalServiceClient;
import graphql.GraphQLException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * GraphQLController is a Spring {@code @Controller} that handles incoming GraphQL queries and
 * mutations. It acts as the entry point for GraphQL operations, delegating business logic to
 * various service layers. This controller uses Spring for GraphQL annotations like
 * {@code @QueryMapping}, {@code @MutationMapping}, and {@code @SchemaMapping} to map GraphQL
 * operations to Java methods.
 *
 * <p>It also integrates Resilience4j for fault tolerance patterns such as Rate Limiting, Circuit
 * Breaking, and Time Limiting to enhance the robustness of external service calls.
 */
@Controller
@RateLimiter(
    name =
        "graphql") // Applies a rate limiter named 'graphql' to all methods within this controller.
public class GraphQLController {

  private final UserService userService;
  private final PostService postService;
  private final ExternalServiceClient externalServiceClient;

  /**
   * Constructs the GraphQLController with necessary service dependencies. Spring's dependency
   * injection automatically provides instances of UserService, PostService, and
   * ExternalServiceClient.
   *
   * @param userService The service responsible for user-related business logic.
   * @param postService The service responsible for post-related business logic.
   * @param externalServiceClient The client for interacting with external services.
   */
  @Autowired
  public GraphQLController(
      UserService userService,
      PostService postService,
      ExternalServiceClient externalServiceClient) {
    this.userService = userService;
    this.postService = postService;
    this.externalServiceClient = externalServiceClient;
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~ Queries ~~~~~~~~~~~~~~~~~~~~~~~~~~

  @QueryMapping
  public List<User> users() {
    return userService.findAll();
  }

  @QueryMapping
  public User user(@Argument Long id) {
    return userService
        .findById(id)
        .orElseThrow(() -> new GraphQLException("User not found with ID: " + id));
  }

  @QueryMapping
  public User userByUsername(@Argument String username) {
    return userService
        .findByUsername(username)
        .orElseThrow(() -> new GraphQLException("User not found with username: " + username));
  }

  @QueryMapping
  public List<User> searchUsers(@Argument String search) {
    return userService.searchUsers(search);
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<User> usersByRole(@Argument User.Role role) {
    return userService.findByRole(role);
  }

  /**
   * Fetches the currently authenticated user's details. This query requires the user to be
   * authenticated. It retrieves the user directly from the security principal, avoiding an extra
   * database lookup.
   *
   * @param authentication The Spring Security {@link Authentication} object.
   * @return The {@link User} object representing the authenticated user.
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  public User me(Authentication authentication) {
    // The User object is stored as the principal by our custom JwtRequestFilter.
    return (User) authentication.getPrincipal();
  }

  @QueryMapping
  public List<Post> posts() {
    return postService.findAll();
  }

  @QueryMapping
  public Post post(@Argument Long id) {
    return postService
        .findById(id)
        .orElseThrow(() -> new GraphQLException("Post not found with ID: " + id));
  }

  @QueryMapping
  public List<Post> postsByUser(@Argument Long userId) {
    return postService.findByUserId(userId);
  }

  @QueryMapping
  public List<Post> searchPosts(@Argument String search) {
    return postService.searchPosts(search);
  }

  @QueryMapping
  public String health() {
    return "OK";
  }

  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  @CircuitBreaker(name = "externalService", fallbackMethod = "getCombinedDataFallback")
  @TimeLimiter(name = "externalService")
  public Mono<CombinedDataResponse> getCombinedData(@Argument String id) {
    String token = extractJwt();
    return Mono.zip(
            externalServiceClient.callServiceA(id, token).timeout(Duration.ofSeconds(2)),
            externalServiceClient.callServiceB(id, token).timeout(Duration.ofSeconds(2)))
        .map(
            tuple ->
                new CombinedDataResponse(tuple.getT1().getData(), tuple.getT2().getData(), null));
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~ Mutations ~~~~~~~~~~~~~~~~~~~~~~~~~~

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  public User updateUser(@Argument Long id, @Argument("user") UserUpdateDTO userUpdateDTO) {
    return userService.updateUser(id, userUpdateDTO);
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~ Schema Mappings ~~~~~~~~~~~~~~~~~~~~~~~

  @SchemaMapping
  public List<Post> posts(User user) {
    return postService.findByUser(user);
  }

  /**
   * Resolves the 'postCount' field for a {@link User} object. Returns a Long to prevent potential
   * integer overflow for users with a very large number of posts.
   *
   * <p><b>Note:</b> Ensure the GraphQL schema for the `postCount` field is updated from `Int!` to
   * `Long!` to match this change.
   *
   * @param user The {@link User} object for which to count posts.
   * @return The number of posts as a Long.
   */
  @SchemaMapping
  public Long postCount(User user) {
    return postService.countByUser(user);
  }

  @SchemaMapping
  public User user(Post post) {
    return post.getUser();
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~ Helper Methods ~~~~~~~~~~~~~~~~~~~~~~~

  private String extractJwt() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return (String) auth.getCredentials();
  }

  public Mono<CombinedDataResponse> getCombinedDataFallback(String id, Throwable ex) {
    return Mono.just(
        new CombinedDataResponse(null, null, Collections.singletonList(ex.getMessage())));
  }
}
