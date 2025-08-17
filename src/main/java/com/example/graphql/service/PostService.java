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

/**
 * A service layer for managing {@link Post} entities.
 *
 * <p>This class encapsulates the business logic for post-related operations, coordinating with the
 * {@link PostRepository}. It also implements caching and resilience patterns (Circuit Breaker,
 * Retry) to ensure performance and stability.
 */
@Service
@Transactional
public class PostService {

  private static final Logger logger = LoggerFactory.getLogger(PostService.class);

  private final PostRepository postRepository;

  /**
   * Best Practice: Use constructor injection for required dependencies. This makes the component's
   * dependencies explicit and improves testability.
   *
   * @param postRepository The repository for post data access.
   */
  @Autowired
  public PostService(PostRepository postRepository) {
    this.postRepository = postRepository;
  }

  /**
   * Retrieves a single post by its ID. This method is cached and protected by a circuit breaker.
   *
   * @param id The unique identifier of the post.
   * @return An {@link Optional} containing the post if found, otherwise empty.
   */
  @Cacheable(cacheNames = "posts", key = "#id")
  @CircuitBreaker(name = "postService", fallbackMethod = "findByIdFallback")
  public Optional<Post> findById(Long id) {
    return postRepository.findById(id);
  }

  /**
   * Fallback method for {@link #findById(Long)}. Executed when the circuit breaker is open. Logs
   * the failure for observability.
   *
   * @param id The ID of the post that was requested.
   * @param ex The exception that triggered the fallback.
   * @return An empty {@link Optional} to indicate failure gracefully.
   */
  public Optional<Post> findByIdFallback(Long id, Throwable ex) {
    logger.warn("Fallback for findById(id={}) triggered. Reason: {}", id, ex.getMessage());
    return Optional.empty();
  }

  /**
   * Retrieves all posts, ordered by creation date. This method is cached and protected by
   * resilience patterns.
   *
   * @return A list of all posts.
   */
  @Cacheable(cacheNames = "allPosts") // Use a separate cache for the "all posts" query
  @CircuitBreaker(name = "postService", fallbackMethod = "findAllFallback")
  @Retry(name = "postService")
  public List<Post> findAll() {
    return postRepository.findAllOrderByCreatedAtDesc();
  }

  /**
   * Fallback method for {@link #findAll()}.
   *
   * @param ex The exception that triggered the fallback.
   * @return An empty list to prevent application failure.
   */
  public List<Post> findAllFallback(Throwable ex) {
    logger.warn("Fallback for findAll() triggered. Reason: {}", ex.getMessage());
    return List.of();
  }

  /**
   * Retrieves all posts created by a specific user. The results are cached using the user's ID as
   * the key.
   *
   * @param user The author of the posts.
   * @return A list of posts by the given user.
   */
  @Cacheable(cacheNames = "postsByUser", key = "#user.id")
  public List<Post> findByUser(User user) {
    return postRepository.findByUser(user);
  }

  /**
   * Retrieves all posts by a specific user ID.
   *
   * @param userId The ID of the author.
   * @return A list of posts for the given user ID.
   */
  @Cacheable(cacheNames = "postsByUser", key = "#userId")
  public List<Post> findByUserId(Long userId) {
    return postRepository.findByUserId(userId);
  }

  /**
   * Retrieves a paginated list of posts for a user. This method is not cached because pagination
   * parameters can be highly dynamic.
   *
   * @param user The author of the posts.
   * @param pageable Pagination and sorting information.
   * @return A {@link Page} of posts.
   */
  public Page<Post> findByUserPaginated(User user, Pageable pageable) {
    return postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
  }

  /**
   * Creates a new post and evicts relevant caches to ensure data consistency.
   *
   * @param post The post entity to be saved. Must have a non-null user.
   * @return The saved post with its generated ID.
   */
  // Note: Evicting all entries is a simple strategy. For high-performance systems,
  // consider a more targeted eviction (e.g., evicting only the specific user's cache).
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "allPosts", allEntries = true),
        @CacheEvict(cacheNames = "postsByUser", key = "#post.user.id")
      })
  public Post createPost(Post post) {
    if (post.getUser() == null) {
      throw new IllegalArgumentException("Post must be associated with a user.");
    }
    return postRepository.save(post);
  }

  /**
   * Updates an existing post's title and content. Evicts caches to reflect the changes.
   *
   * @param id The ID of the post to update.
   * @param postUpdate An object containing the new title and/or content.
   * @return The updated post.
   * @throws EntityNotFoundException if no post with the given ID is found.
   */
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "posts", key = "#id"),
        @CacheEvict(cacheNames = "allPosts", allEntries = true),
        @CacheEvict(cacheNames = "postsByUser", allEntries = true) // Inefficient, but safe.
      })
  public Post updatePost(Long id, Post postUpdate) {
    Post post =
        postRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Post not found with ID: " + id));

    if (postUpdate.getTitle() != null) {
      post.setTitle(postUpdate.getTitle());
    }
    if (postUpdate.getContent() != null) {
      post.setContent(postUpdate.getContent());
    }
    return postRepository.save(post);
  }

  /**
   * Deletes a post by its ID and evicts all related caches.
   *
   * @param id The ID of the post to delete.
   * @throws EntityNotFoundException if no post with the given ID is found.
   */
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "posts", key = "#id"),
        @CacheEvict(cacheNames = "allPosts", allEntries = true),
        @CacheEvict(cacheNames = "postsByUser", allEntries = true)
      })
  public void deletePost(Long id) {
    if (!postRepository.existsById(id)) {
      throw new EntityNotFoundException("Post not found with ID: " + id);
    }
    postRepository.deleteById(id);
  }

  /**
   * Searches for posts based on a keyword in the title or content. This query is dynamic and
   * therefore not cached.
   *
   * @param search The keyword to search for.
   * @return A list of matching posts.
   */
  public List<Post> searchPosts(String search) {
    return postRepository.searchPosts(search);
  }

  /**
   * Counts the total number of posts for a given user. This is more efficient than fetching all
   * posts and getting the list size.
   *
   * @param user The user whose posts are to be counted.
   * @return The total number of posts for the user.
   */
  public long countByUser(User user) {
    return postRepository.countByUser(user);
  }
}
