package com.example.graphql.controller;

import com.example.graphql.entity.Post;
import com.example.graphql.publisher.PostPublisher;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class PostSubscriptionController {
    private final PostPublisher postPublisher;

    public PostSubscriptionController(PostPublisher postPublisher) {
        this.postPublisher = postPublisher;
    }

    @SubscriptionMapping
    public Flux<Post> postAdded() {
        return postPublisher.flux();
    }
}
