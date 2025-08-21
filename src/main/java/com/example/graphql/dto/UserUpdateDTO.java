package com.example.graphql.dto;

import com.example.graphql.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Optional;

public class UserUpdateDTO {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

/**
 * DTO for updating user information via GraphQL mutations.
 */
    @Email(message = "Invalid email format")
    private String email;

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Apply updates to an existing User entity
    public void applyUpdatesTo(User user) {
        getUsername().ifPresent(user::setUsername);
        getEmail().ifPresent(user::setEmail);
    }
}
