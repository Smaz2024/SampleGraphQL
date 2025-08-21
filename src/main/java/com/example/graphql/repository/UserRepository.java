/**
 * JPA repository for User entities.
 * Provides CRUD and custom search operations.
 */
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
 * Repository interface for managing {@link User} entities.
 *
 * <p>Extends {@link JpaRepository} to provide CRUD operations, pagination, and sorting
 * capabilities. It also includes custom queries for searching by username, email, and role.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    Page<User> findByRole(User.Role role, Pageable pageable);

    @Query(
            """
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    List<User> searchUsers(@Param("search") String search);

    @Query(
            """
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);
}
