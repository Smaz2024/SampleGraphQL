package com.example.graphql.repository;

import com.example.graphql.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>This interface provides the mechanism for all database operations related to users, including
 * standard CRUD (Create, Read, Update, Delete) functionality. It also defines custom query methods
 * for application-specific use cases like searching by username, email, or role, with support for
 * pagination.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by their unique username. This is a common operation for login and profile
   * retrieval.
   *
   * @param username The username to search for. Must not be null.
   * @return An {@link Optional} containing the found {@link User}, or an empty Optional if no user
   *     is found.
   */
  Optional<User> findByUsername(String username);

  /**
   * Finds a user by their unique email address.
   *
   * @param email The email address to search for. Must not be null.
   * @return An {@link Optional} containing the found {@link User}, or an empty Optional if not
   *     found.
   */
  Optional<User> findByEmail(String email);

  /**
   * Checks if a user with the given username exists in the database. This is more efficient than
   * fetching the entire user entity.
   *
   * @param username The username to check.
   * @return {@code true} if a user with the given username exists, {@code false} otherwise.
   */
  boolean existsByUsername(String username);

  /**
   * Checks if a user with the given email exists in the database. This is more efficient than
   * fetching the entire user entity.
   *
   * @param email The email to check.
   * @return {@code true} if a user with the given email exists, {@code false} otherwise.
   */
  boolean existsByEmail(String email);

  /**
   * Finds all users assigned a specific role.
   *
   * @param role The {@link User.Role} to filter by.
   * @return A {@link List} of all users matching the specified role.
   */
  List<User> findByRole(User.Role role);

  /**
   * Finds a paginated list of users assigned a specific role.
   *
   * @param role The {@link User.Role} to filter by.
   * @param pageable The {@link Pageable} object containing pagination and sorting information.
   * @return A {@link Page} of users matching the specified role.
   */
  Page<User> findByRole(User.Role role, Pageable pageable);

  /**
   * Performs a case-insensitive search for users where the search term appears in either the
   * username or the email address.
   *
   * @param search The search term to match against username and email.
   * @return A {@link List} of users that match the search criteria.
   */
  @Query(
      """
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
  List<User> searchUsers(@Param("search") String search);

  /**
   * Performs a paginated, case-insensitive search for users by username or email.
   *
   * @param search The search term to match against username and email.
   * @param pageable The {@link Pageable} object for pagination and sorting.
   * @return A {@link Page} of users that match the search criteria.
   */
  @Query(
      """
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
  Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
