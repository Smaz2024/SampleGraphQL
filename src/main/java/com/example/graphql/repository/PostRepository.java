package com.example.graphql.repository;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Post} entities.
 *
 * <p>This interface provides the mechanism for all database operations related to posts, including
 * creating, reading, updating, and deleting (CRUD) records. It also includes custom queries for
 * specific use cases like searching and paginated fetching.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

  /**
   * Finds all posts authored by a specific user.
   *
   * <p>{@code @EntityGraph} is used to solve the N+1 query problem by telling JPA to fetch the
   * associated 'user' entity in the same query as the posts, avoiding separate queries for each
   * post's author.
   *
   * @param user The user entity whose posts are to be retrieved.
   * @return A list of posts by the given user.
   */
  @EntityGraph(attributePaths = {"user"})
  List<Post> findByUser(User user);

  /**
   * Finds a paginated list of posts for a given user, ordered by creation date in descending order.
   * This is useful for displaying a user's post history in a chronological feed.
   *
   * @param user The user whose posts are to be fetched.
   * @param pageable The pagination information (page number, size, and sort).
   * @return A {@link Page} of posts.
   */
  @EntityGraph(attributePaths = {"user"})
  Page<Post> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  /**
   * Finds a post by its unique ID. This is a standard JPA repository method, overridden here for
   * clarity.
   *
   * @param id The ID of the post to find.
   * @return An {@link Optional} containing the post if found, or empty otherwise.
   */
  Optional<Post> findById(Long id);

  /**
   * Finds all posts associated with a specific user ID. Note: This method does not use an
   * EntityGraph and may lead to N+1 problems if the user object for each post is accessed later.
   *
   * @param userId The ID of the user.
   * @return A list of posts for the given user ID.
   */
  List<Post> findByUserId(Long userId);

  /**
   * Retrieves all posts from the database, ordered by their creation date in descending order.
   *
   * @return A chronologically sorted list of all posts.
   */
  @EntityGraph(attributePaths = {"user"})
  @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
  List<Post> findAllOrderByCreatedAtDesc();

  /**
   * Performs a case-insensitive search for posts where the search term appears in either the title
   * or the content.
   *
   * @param search The search term to look for.
   * @return A list of posts matching the search criteria.
   */
  @EntityGraph(attributePaths = {"user"})
  @Query(
      "SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) "
          + "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%'))")
  List<Post> searchPosts(@Param("search") String search);

  /**
   * Counts the total number of posts authored by a specific user. This is more efficient than
   * fetching all posts and getting the list size.
   *
   * @param user The user whose posts are to be counted.
   * @return The total number of posts for the user.
   */
  long countByUser(User user);
}
