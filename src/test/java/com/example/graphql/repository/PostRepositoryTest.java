package com.example.graphql.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.example.graphql.entity.Post;
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
public class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Post testPost1;
    private Post testPost2;

    @BeforeEach
    void setUp() {
        // Create and persist a test user
        testUser = new User("testuser", "test@example.com", "password", User.Role.USER);
        entityManager.persist(testUser);

        // Create and persist test posts
        testPost1 = new Post("Test Post 1", "Content 1", testUser);
        entityManager.persist(testPost1);

        testPost2 = new Post("Test Post 2", "Content 2", testUser);
        entityManager.persist(testPost2);

        // Flush to ensure data is saved
        entityManager.flush();
    }

    @Test
    void testFindByUser() {
        // Act
        List<Post> posts = postRepository.findByUser(testUser);

        // Assert
        assertNotNull(posts);
        assertEquals(2, posts.size());
        assertTrue(posts.contains(testPost1));
        assertTrue(posts.contains(testPost2));
    }

    @Test
    void testFindByUserId() {
        // Act
        List<Post> posts = postRepository.findByUserId(testUser.getId());

        // Assert
        assertNotNull(posts);
        assertEquals(2, posts.size());
        assertTrue(posts.contains(testPost1));
        assertTrue(posts.contains(testPost2));
    }

    @Test
    void testFindByUserOrderByCreatedAtDesc() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Post> postPage = postRepository.findByUserOrderByCreatedAtDesc(testUser, pageable);

        // Assert
        assertNotNull(postPage);
        assertEquals(2, postPage.getContent().size());
        // Verify order - most recent post should be first
        assertEquals(testPost2, postPage.getContent().get(0));
        assertEquals(testPost1, postPage.getContent().get(1));
    }

    @Test
    void testFindAllOrderByCreatedAtDesc() {
        // Act
        List<Post> posts = postRepository.findAllOrderByCreatedAtDesc();

        // Assert
        assertNotNull(posts);
        assertTrue(posts.size() >= 2);
        // Verify order - most recent post should be first
        assertEquals(testPost2, posts.get(0));
        assertEquals(testPost1, posts.get(1));
    }

    @Test
    void testCountByUser() {
        // Act
        long postCount = postRepository.countByUser(testUser);

        // Assert
        assertEquals(2, postCount);
    }

    @Test
    void testCount() {
        // Act
        long totalPosts = postRepository.count();

        // Assert
        assertTrue(totalPosts >= 2);
    }

    @Test
    void testSavePost() {
        // Arrange
        Post newPost = new Post("New Post", "New Content", testUser);

        // Act
        Post savedPost = postRepository.save(newPost);

        // Assert
        assertNotNull(savedPost);
        assertNotNull(savedPost.getId());
        assertEquals("New Post", savedPost.getTitle());
        assertEquals("New Content", savedPost.getContent());
        assertEquals(testUser, savedPost.getUser());
    }

    @Test
    void testFindById() {
        // Act
        Optional<Post> foundPost = postRepository.findById(testPost1.getId());

        // Assert
        assertTrue(foundPost.isPresent());
        assertEquals(testPost1, foundPost.get());
    }

    @Test
    void testDeleteById() {
        // Act
        postRepository.deleteById(testPost1.getId());
        Optional<Post> deletedPost = postRepository.findById(testPost1.getId());

        // Assert
        assertTrue(deletedPost.isEmpty());
    }
}
