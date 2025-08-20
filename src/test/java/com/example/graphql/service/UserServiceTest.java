package com.example.graphql.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "password", User.Role.USER);
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        // Act
        List<User> users = userService.getAllUsers();

        // Assert
        assertNotNull(users);
        assertEquals(1, users.size());
        assertEquals(testUser, users.get(0));
        verify(userRepository).findAll();
    }

    @Test
    void testGetUserByEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        User user = userService.getUserByEmail("test@example.com");

        // Assert
        assertNotNull(user);
        assertEquals(testUser, user);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void testGetUserById() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User user = userService.getUserById(1L);

        // Assert
        assertNotNull(user);
        assertEquals(testUser, user);
        verify(userRepository).findById(1L);
    }

    @Test
    void testCreateUser() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User createdUser = userService.createUser("testuser", "test@example.com", "password", "USER");

        // Assert
        assertNotNull(createdUser);
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        assertEquals(testUser.getEmail(), createdUser.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUserGraphQL() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User updatedUser = userService.updateUserGraphQL(1L, "newusername", "new@example.com", "newpassword", "ADMIN");

        // Assert
        assertNotNull(updatedUser);
        assertEquals("newusername", updatedUser.getUsername());
        assertEquals("new@example.com", updatedUser.getEmail());
        assertEquals(User.Role.ADMIN, updatedUser.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testDeleteUserGraphQL() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean result = userService.deleteUserGraphQL(1L);

        // Assert
        assertTrue(result);
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void testCountUsers() {
        // Arrange
        when(userRepository.count()).thenReturn(1L);

        // Act
        long count = userService.countUsers();

        // Assert
        assertEquals(1L, count);
        verify(userRepository).count();
    }
}
