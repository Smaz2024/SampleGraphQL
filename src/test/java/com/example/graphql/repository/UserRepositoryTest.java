package com.example.graphql.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.example.graphql.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        // Create and persist test users
        testUser1 = new User("testuser1", "test1@example.com", "password1", User.Role.USER);
        testUser2 = new User("testuser2", "test2@example.com", "password2", User.Role.ADMIN);

        entityManager.persist(testUser1);
        entityManager.persist(testUser2);

        // Flush to ensure data is saved
        entityManager.flush();
    }

    @Test
    void testFindByUsername() {
        // Act
        Optional<User> foundUser = userRepository.findByUsername("testuser1");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(testUser1, foundUser.get());
    }

    @Test
    void testFindByEmail() {
        // Act
        Optional<User> foundUser = userRepository.findByEmail("test2@example.com");

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals(testUser2, foundUser.get());
    }

    @Test
    void testExistsByUsername() {
        // Act
        boolean existsUser1 = userRepository.existsByUsername("testuser1");
        boolean existsUser3 = userRepository.existsByUsername("nonexistentuser");

        // Assert
        assertTrue(existsUser1);
        assertFalse(existsUser3);
    }

    @Test
    void testExistsByEmail() {
        // Act
        boolean existsEmail1 = userRepository.existsByEmail("test1@example.com");
        boolean existsEmail3 = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertTrue(existsEmail1);
        assertFalse(existsEmail3);
    }

    @Test
    void testFindByRole() {
        // Act
        List<User> usersByRole = userRepository.findByRole(User.Role.ADMIN);

        // Assert
        assertNotNull(usersByRole);
        assertEquals(1, usersByRole.size());
        assertEquals(testUser2, usersByRole.get(0));
    }

    @Test
    void testFindByRoleWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> userPage = userRepository.findByRole(User.Role.USER, pageable);

        // Assert
        assertNotNull(userPage);
        assertEquals(1, userPage.getContent().size());
        assertEquals(testUser1, userPage.getContent().get(0));
    }

    @Test
    void testSearchUsers() {
        // Act
        List<User> searchResults = userRepository.searchUsers("test1");

        // Assert
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        assertEquals(testUser1, searchResults.get(0));
    }

    @Test
    void testSearchUsersWithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<User> searchResults = userRepository.searchUsers("test", pageable);

        // Assert
        assertNotNull(searchResults);
        assertEquals(2, searchResults.getContent().size());
        assertTrue(searchResults.getContent().contains(testUser1));
        assertTrue(searchResults.getContent().contains(testUser2));
    }

    @Test
    void testSaveUser() {
        // Arrange
        User newUser = new User("newuser", "new@example.com", "newpassword", User.Role.USER);

        // Act
        User savedUser = userRepository.save(newUser);

        // Assert
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("new@example.com", savedUser.getEmail());
        assertEquals(User.Role.USER, savedUser.getRole());
    }

    @Test
    void testDeleteUser() {
        // Act
        userRepository.delete(testUser1);
        Optional<User> deletedUser = userRepository.findById(testUser1.getId());

        // Assert
        assertTrue(deletedUser.isEmpty());
    }

    @Test
    void testCount() {
        // Act
        long userCount = userRepository.count();

        // Assert
        assertEquals(2, userCount);
    }
}
