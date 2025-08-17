package com.example.graphql;

import com.example.graphql.service.PostService;
import com.example.graphql.service.UserService;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableTransactionManagement
@EntityScan(basePackages = "com.example.graphql.entity")
@EnableJpaRepositories(basePackages = "com.example.graphql.repository")
public class GraphQLPocApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GraphQLPocApplication.class);

    private final UserService userService;
    private final PostService postService;
    private final Environment env;

    public GraphQLPocApplication(UserService userService, PostService postService, Environment env) {
        this.userService = userService;
        this.postService = postService;
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(GraphQLPocApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // Only log seeding info for the 'dev' profile. If anything dev specific is needed, then add
        // here.
        if (Arrays.asList(env.getActiveProfiles()).contains("dev")) {
            log.info("Startup: DEV profile active. Data will be seeded via data.sql.");
        } else {
            log.info("Startup: Skipping data seeding for non-dev profile");
        }
    }
}
