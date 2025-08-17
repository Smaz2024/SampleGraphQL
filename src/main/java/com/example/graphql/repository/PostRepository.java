package com.example.graphql.repository;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link Post} entities.
 *
 * <p>Provides CRUD operations and custom queries for posts, optimized with {@link EntityGraph} to
 * prevent N+1 query problems when accessing the associated {@link User}.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Finds all posts authored by a given user.
     *
     * @param user The user whose posts should be retrieved.
     * @return A list of posts.
     */
    @EntityGraph(attributePaths = {"user"})
    List<Post> findByUser(User user);

    /**
     * Retrieves a paginated list of posts authored by a given user, ordered by creation date (latest
     * first).
     *
     * @param user The user whose posts to retrieve.
     * @param pageable Pagination info.
     * @return A page of posts.
     */
    @EntityGraph(attributePaths = {"user"})
    Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Retrieves all posts authored by the given user ID.
     *
     * @param userId The ID of the user.
     * @return A list of posts.
     */
    @EntityGraph(attributePaths = {"user"})
    List<Post> findByUserId(Long userId);

    /**
     * Retrieves all posts sorted by creation date in descending order.
     *
     * @return A list of posts (newest first).
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findAllOrderByCreatedAtDesc();

    /**
     * Performs a case-insensitive search across post titles and content.
     *
     * @param search The search term.
     * @return A list of matching posts.
     *
     * @EntityGraph(attributePaths = {"user"})
     * @Query(
     * value =
     * """
     * SELECT p.* FROM posts p
     * WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
     * OR LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
     * """,
     * nativeQuery = true)
     * List<Post> searchPostsNative(@Param("searchTerm") String searchTerm);
     */

    /**
     * Counts the total number of posts authored by the given user.
     *
     * @param user The user.
     * @return The number of posts.
     */
    long countByUser(User user);

    /**
     * Counts the total number of posts.
     *
     * @return The total number of posts.
     */
    long count();
}
