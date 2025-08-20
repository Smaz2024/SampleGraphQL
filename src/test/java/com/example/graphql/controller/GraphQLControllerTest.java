package com.example.graphql.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.graphql.entity.Post;
import com.example.graphql.entity.User;
import com.example.graphql.service.PostService;
import com.example.graphql.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GraphQLControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PostService postService;

    @InjectMocks
    private GraphQLController graphQLController;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", User.Role.USER);
        // ID is auto-generated; for test equality, you may mock getId if needed
        testPost = new Post("Test Post", "Test Content", testUser);
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        List<User> expectedUsers = List.of(testUser);
        when(userService.getAllUsers()).thenReturn(expectedUsers);

        // Act
        List<User> actualUsers = graphQLController.users();

        // Assert
        assertNotNull(actualUsers);
        assertEquals(1, actualUsers.size());
        assertEquals(testUser, actualUsers.get(0));
        verify(userService).getAllUsers();
    }

    @Test
    void testGetAllPosts() {
        // Arrange
        List<Post> expectedPosts = List.of(testPost);
        when(postService.getAllPosts()).thenReturn(expectedPosts);

        // Act
        List<Post> actualPosts = graphQLController.posts();

        // Assert
        assertNotNull(actualPosts);
        assertEquals(1, actualPosts.size());
        assertEquals(testPost, actualPosts.get(0));
        verify(postService).getAllPosts();
    }

    @Test
    void testCreateUser() {
        // Arrange
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "password";
        String role = "USER";
        User expectedUser = new User(username, email, password, User.Role.USER);

        when(userService.createUser(username, email, password, role)).thenReturn(expectedUser);

        // Act
        User createdUser = graphQLController.createUser(username, email, password, role);

        // Assert
        assertNotNull(createdUser);
        assertEquals(expectedUser, createdUser);
        verify(userService).createUser(username, email, password, role);
    }

    @Test
    void testCreatePost() {
        // Arrange
        String title = "New Post";
        String content = "New Post Content";
        String authorEmail = "test@example.com";

        when(userService.getUserByEmail(authorEmail)).thenReturn(testUser);
        when(postService.createPost(title, content, authorEmail)).thenReturn(testPost);

        // Act
        Post createdPost = graphQLController.createPost(title, content, authorEmail);

        // Assert
        assertNotNull(createdPost);
        assertEquals(testPost, createdPost);
        verify(userService).getUserByEmail(authorEmail);
        verify(postService).createPost(title, content, authorEmail);
    }
}
