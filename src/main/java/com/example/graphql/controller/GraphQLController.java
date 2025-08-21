package com.example.graphql.controller;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.service.PostService;
import com.example.graphql.service.UserService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Controller
public class GraphQLController {

    /**
     * Main GraphQL controller.
     * Handles queries, mutations, and subscriptions for users and posts.
     */
    private final UserService userService;
    private final PostService postService;

    /**
     * Main GraphQL controller.
     * Handles queries, mutations, and subscriptions for users and posts.
     */
    public GraphQLController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    // --- User queries ---
    @QueryMapping
    public List<User> users() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public User userByEmail(@Argument String email) {
        return userService.getUserByEmail(email);
    }

    @QueryMapping
    public User userById(@Argument Long id) {
        return userService.getUserById(id);
    }

    @QueryMapping
    public List<User> searchUsers(@Argument String search) {
        return userService.searchUsers(search);
    }

    @QueryMapping
    public long countUsers() {
        return userService.countUsers();
    }

    // --- User mutations ---
    @MutationMapping
    public User createUser(
            @Argument String username, @Argument String email, @Argument String password, @Argument String role) {
        return userService.createUser(username, email, password, role);
    }

    @MutationMapping
    public User updateUser(
            @Argument Long id,
            @Argument String username,
            @Argument String email,
            @Argument String password,
            @Argument String role) {
        return userService.updateUserGraphQL(id, username, email, password, role);
    }

    @MutationMapping
    public boolean deleteUser(@Argument Long id) {
        return userService.deleteUserGraphQL(id);
    }

    // --- Post queries ---
    @QueryMapping
    public List<Post> posts() {
        return postService.getAllPosts();
    }

    @QueryMapping
    public Post postById(@Argument Long id) {
        return postService.getPostById(id);
    }

    @QueryMapping
    public Flux<Post> postsByAuthor(@Argument String authorEmail) {
        return postService.getPostsByAuthorEmailReactive(authorEmail);
    }

    @QueryMapping
    public List<Post> searchPosts(@Argument String search) {
        return List.of(new Post(
                "Search Not Implemented",
                "This is a placeholder response. Search functionality is not implemented yet.",
                null));
        // postService.searchPosts(search);
    }

    @QueryMapping
    public Mono<Long> countPosts() {
        return Mono.fromCallable(() -> postService.countPosts())
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    // --- Post mutations ---
    @MutationMapping
    public Mono<Post> createPost(@Argument String title, @Argument String content, @Argument String authorEmail) {
        return postService.createPostReactive(title, content, authorEmail);
    }

    @MutationMapping
    public Mono<Post> updatePost(@Argument Long id, @Argument String title, @Argument String content) {
        return postService.updatePostReactive(id, title, content);
    }

    @MutationMapping
    public Mono<Boolean> deletePost(@Argument Long id) {
        return postService.deletePostReactive(id);
    }

    // --- Subscriptions ---
    @SubscriptionMapping
    public Flux<Post> postAdded() {
        return postService.postFlux();
    }
}
