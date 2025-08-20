package com.example.graphql.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", User.Role.USER);
        testPost = new Post("Test Post", "Test Content", testUser);
        // Ensure userService is injected for all tests (fixes NPE)
        try {
            java.lang.reflect.Field userServiceField = PostService.class.getDeclaredField("userService");
            userServiceField.setAccessible(true);
            userServiceField.set(postService, userService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject userService into PostService", e);
        }
    }

    @Test
    void testFindById() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        // Act
        Optional<Post> foundPost = postService.findById(1L);

        // Assert
        assertTrue(foundPost.isPresent());
        assertEquals(testPost, foundPost.get());
        verify(postRepository).findById(1L);
    }

    @Test
    void testGetById() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        // Act
        Post foundPost = postService.getById(1L);

        // Assert
        assertNotNull(foundPost);
        assertEquals(testPost, foundPost);
        verify(postRepository).findById(1L);
    }

    @Test
    void testFindAll() {
        // Arrange
        List<Post> expectedPosts = List.of(testPost);
        when(postRepository.findAllOrderByCreatedAtDesc()).thenReturn(expectedPosts);

        // Act
        List<Post> actualPosts = postService.findAll();

        // Assert
        assertNotNull(actualPosts);
        assertEquals(1, actualPosts.size());
        assertEquals(testPost, actualPosts.get(0));
        verify(postRepository).findAllOrderByCreatedAtDesc();
    }

    @Test
    void testFindByUser() {
        // Arrange
        List<Post> expectedPosts = List.of(testPost);
        when(postRepository.findByUser(testUser)).thenReturn(expectedPosts);

        // Act
        List<Post> actualPosts = postService.findByUser(testUser);

        // Assert
        assertNotNull(actualPosts);
        assertEquals(1, actualPosts.size());
        assertEquals(testPost, actualPosts.get(0));
        verify(postRepository).findByUser(testUser);
    }

    @Test
    void testFindByUserId() {
        // Arrange
        List<Post> expectedPosts = List.of(testPost);
        when(postRepository.findByUserId(1L)).thenReturn(expectedPosts);

        // Act
        List<Post> actualPosts = postService.findByUserId(1L);

        // Assert
        assertNotNull(actualPosts);
        assertEquals(1, actualPosts.size());
        assertEquals(testPost, actualPosts.get(0));
        verify(postRepository).findByUserId(1L);
    }

    @Test
    void testFindByUserPaginated() {
        // Arrange
        Pageable pageable = mock(Pageable.class);
        Page<Post> expectedPage = new PageImpl<>(List.of(testPost));
        when(postRepository.findByUserOrderByCreatedAtDesc(testUser, pageable)).thenReturn(expectedPage);

        // Act
        Page<Post> actualPage = postService.findByUserPaginated(testUser, pageable);

        // Assert
        assertNotNull(actualPage);
        assertEquals(1, actualPage.getContent().size());
        assertEquals(testPost, actualPage.getContent().get(0));
        verify(postRepository).findByUserOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    void testGetPostsByAuthorEmail() {
        // Arrange
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(postRepository.findByUser(testUser)).thenReturn(List.of(testPost));

        // Act
        List<Post> actualPosts = postService.getPostsByAuthorEmail("test@example.com");

        // Assert
        assertNotNull(actualPosts);
        assertEquals(1, actualPosts.size());
        assertEquals(testPost, actualPosts.get(0));
        verify(userService).getUserByEmail("test@example.com");
        verify(postRepository).findByUser(testUser);
    }

    @Test
    void testCreatePost() {
        // Arrange
        when(postRepository.save(testPost)).thenReturn(testPost);

        // Act
        Post createdPost = postService.createPost(testPost);

        // Assert
        assertNotNull(createdPost);
        assertEquals(testPost, createdPost);
        verify(postRepository).save(testPost);
    }

    @Test
    void testCreatePostWithDetails() {
        // Arrange
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        // Act
        Post createdPost = postService.createPost("Test Title", "Test Content", "test@example.com");

        // Assert
        assertNotNull(createdPost);
        assertEquals(testPost, createdPost);
        verify(userService).getUserByEmail("test@example.com");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void testUpdatePost() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(testPost)).thenReturn(testPost);

        Post updatePost = new Post("Updated Title", "Updated Content", testUser);

        // Act
        Post updatedPost = postService.updatePost(1L, updatePost);

        // Assert
        assertNotNull(updatedPost);
        assertEquals("Updated Title", updatedPost.getTitle());
        assertEquals("Updated Content", updatedPost.getContent());
        verify(postRepository).findById(1L);
        verify(postRepository).save(testPost);
    }

    @Test
    void testUpdatePostGraphQL() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(testPost)).thenReturn(testPost);

        // Act
        Post updatedPost = postService.updatePostGraphQL(1L, "Updated Title", "Updated Content");

        // Assert
        assertNotNull(updatedPost);
        assertEquals("Updated Title", updatedPost.getTitle());
        assertEquals("Updated Content", updatedPost.getContent());
        verify(postRepository, atLeastOnce()).findById(1L);
        verify(postRepository).save(testPost);
    }

    @Test
    void testDeletePostGraphQL() {
        // Arrange
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        // Act
        boolean result = postService.deletePostGraphQL(1L);

        // Assert
        assertTrue(result);
        verify(postRepository, atLeastOnce()).findById(1L);
        verify(postRepository).deleteById(1L);
    }

    @Test
    void testCountByUser() {
        // Arrange
        when(postRepository.countByUser(testUser)).thenReturn(1L);

        // Act
        long count = postService.countByUser(testUser);

        // Assert
        assertEquals(1L, count);
        verify(postRepository).countByUser(testUser);
    }

    @Test
    void testCountPosts() {
        // Arrange
        when(postRepository.count()).thenReturn(1L);

        // Act
        long count = postService.countPosts();

        // Assert
        assertEquals(1L, count);
        verify(postRepository).count();
    }
}
