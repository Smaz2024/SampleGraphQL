package com.example.graphql;

import com.example.graphql.service.PostService;
import com.example.graphql.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main entry point for the Spring Boot GraphQL Proof of Concept application.
 *
 * <p>This class bootstraps the Spring application
 *
 * <h3>Key Architectural Annotations:</h3>
 *
 * <ul>
 *   <li>{@code @SpringBootApplication}: A convenience annotation that enables auto-configuration,
 *       component scanning, and defines this as a configuration class.
 *   <li>{@code @EnableCaching}: Activates Spring's annotation-driven cache management, crucial for
 *       optimizing performance by caching results from services like {@link UserService} and {@link
 *       PostService}.
 *   <li>{@code @EnableTransactionManagement}: Enables Spring's annotation-driven transaction
 *       management, ensuring that database operations within {@code @Transactional} methods are
 *       atomic and consistent.
 *   <li>{@code @Profile("!prod")}: A powerful feature that restricts this component (specifically
 *       the data seeding logic) to run only when the 'prod' Spring profile is <em>not</em> active.
 *       This is a critical safeguard to prevent sample data from being loaded into a production
 *       database.
 * </ul>
 */
@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
@EntityScan(basePackages = "com.example.graphql.entity")
@EnableJpaRepositories(basePackages = "com.example.graphql.repository")
public class GraphQLPocApplication {

  private static final Logger log = LoggerFactory.getLogger(GraphQLPocApplication.class);

  private final UserService userService;
  private final PostService postService;

  /**
   * Constructs the application, injecting required service dependencies via the constructor. This
   * follows the best practice of constructor injection for mandatory dependencies, ensuring the
   * application cannot start without its core services.
   *
   * @param userService The service for user-related business logic.
   * @param postService The service for post-related business logic.
   */
  public GraphQLPocApplication(UserService userService, PostService postService) {
    this.userService = userService;
    this.postService = postService;
  }

  /**
   * The main method, which serves as the entry point for the Java Virtual Machine (JVM) to run the
   * application. It delegates to Spring Boot's {@link SpringApplication}.
   *
   * @param args Command-line arguments passed to the application.
   */
  public static void main(String[] args) {
    SpringApplication.run(GraphQLPocApplication.class, args);
  }
}
