package com.example.graphql.publisher;

import com.example.graphql.entity.Post;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class PostPublisher {
    private final Sinks.Many<Post> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publish(Post post) {
        sink.tryEmitNext(post);
    }

    public Flux<Post> flux() {
        return sink.asFlux();
    }
}
