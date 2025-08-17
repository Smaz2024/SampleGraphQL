package com.example.graphql.service;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.repository.PostRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    private static final String CB = "postService";

    private final PostRepository postRepository;

    @Autowired
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Autowired
    private UserService userService;

    // ---------- Reads (readOnly transactions) ----------

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "posts", key = "#id")
    @CircuitBreaker(name = CB, fallbackMethod = "findByIdFallback")
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    // Convenience when a Post is expected to exist
    @Transactional(readOnly = true)
    public Post getById(Long id) {
        return postRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "allPosts")
    @CircuitBreaker(name = CB, fallbackMethod = "findAllFallback")
    @Retry(name = CB)
    public List<Post> findAll() {
        return postRepository.findAllOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "postsByUser", key = "#user.id")
    @CircuitBreaker(name = CB, fallbackMethod = "findByUserFallback")
    public List<Post> findByUser(User user) {
        return postRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "postsByUser", key = "#userId")
    @CircuitBreaker(name = CB, fallbackMethod = "findByUserIdFallback")
    public List<Post> findByUserId(Long userId) {
        return postRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<Post> findByUserPaginated(User user, Pageable pageable) {
        return postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    public List<Post> getAllPosts() {
        return findAll();
    }

    public Post getPostById(Long id) {
        return getById(id);
    }

    public List<Post> getPostsByAuthorEmail(String email) {
        User user = userService.getUserByEmail(email);
        return findByUser(user);
    }

    public Post createPost(String title, String content, String authorEmail) {
        User user = userService.getUserByEmail(authorEmail);
        Post post = new Post(title, content, user);
        return createPost(post);
    }

    public Post updatePostGraphQL(Long id, String title, String content) {
        Post post = getById(id);
        if (title != null) post.setTitle(title);
        if (content != null) post.setContent(content);
        return updatePost(id, post);
    }

    public boolean deletePostGraphQL(Long id) {
        if (findById(id).isEmpty()) return false;
        deletePost(id);
        return true;
    }

    /*  @Transactional(readOnly = true)
    public List<Post> searchPosts(String search) {
        return postRepository.searchPosts(search);
    }*/

    @Transactional(readOnly = true)
    public long countByUser(User user) {
        return postRepository.countByUser(user);
    }

    public long countPosts() {
        return postRepository.count();
    }

    // ---------- Writes (default readOnly=false) ----------

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = "allPosts", allEntries = true),
                @CacheEvict(cacheNames = "postsByUser", key = "#post.user.id")
            })
    public Post createPost(Post post) {
        if (post.getUser() == null) {
            throw new IllegalArgumentException("Post must be associated with a user.");
        }
        Post saved = postRepository.save(post);
        log.info("Post {} created by user {}", saved.getId(), saved.getUser().getId());
        return saved;
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = "posts", key = "#id"),
                @CacheEvict(cacheNames = "allPosts", allEntries = true),
                // Targeted eviction for the affected user
                @CacheEvict(cacheNames = "postsByUser", key = "#result.user.id", condition = "#result != null")
            })
    public Post updatePost(Long id, Post postUpdate) {
        Post post = getById(id);

        if (postUpdate.getTitle() != null) post.setTitle(postUpdate.getTitle());
        if (postUpdate.getContent() != null) post.setContent(postUpdate.getContent());

        Post saved = postRepository.save(post);
        log.info("Post {} updated (user {})", saved.getId(), saved.getUser().getId());
        return saved;
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = "posts", key = "#id"),
                @CacheEvict(cacheNames = "allPosts", allEntries = true),
                // Evict by the actual author's id
                @CacheEvict(cacheNames = "postsByUser", key = "#authorId", condition = "#authorId != null")
            })
    public void deletePost(Long id) {
        Post existing = getById(id);
        Long authorId = existing.getUser() != null ? existing.getUser().getId() : null;
        postRepository.deleteById(id);
        log.info("Post {} deleted (user {})", id, authorId);
    }

    // ---------- Fallbacks ----------

    public Optional<Post> findByIdFallback(Long id, Throwable ex) {
        log.warn("Fallback findById(id={}) - {}", id, ex.getMessage());
        return Optional.empty();
    }

    public List<Post> findAllFallback(Throwable ex) {
        log.warn("Fallback findAll() - {}", ex.getMessage());
        return List.of();
    }

    public List<Post> findByUserFallback(User user, Throwable ex) {
        log.warn("Fallback findByUser(userId={}) - {}", user != null ? user.getId() : null, ex.getMessage());
        return List.of();
    }

    public List<Post> findByUserIdFallback(Long userId, Throwable ex) {
        log.warn("Fallback findByUserId(userId={}) - {}", userId, ex.getMessage());
        return List.of();
    }
}
