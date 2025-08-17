package com.example.graphql.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Represents a User entity in the application, mapping to the 'users' table. This class implements
 * Spring Security's {@link UserDetails} interface, allowing it to be seamlessly integrated into the
 * authentication and authorization mechanisms. It holds user credentials, roles, and personal
 * information.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

  /** The unique identifier for the user. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The user's unique username. Used for login and identification. */
  @NotBlank(message = "Username cannot be blank")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  @Column(unique = true, nullable = false)
  private String username;

  /** The user's unique email address. */
  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Email should be valid")
  @Column(unique = true, nullable = false)
  private String email;

  /** The user's hashed password. It is never stored in plain text. */
  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, message = "Password must be at least 8 characters long")
  private String password;

  /** The role of the user, which determines their permissions within the application. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.USER;

  /**
   * The timestamp when the user account was created. Automatically set by Hibernate upon creation.
   */
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  /**
   * The timestamp when the user account was last updated. Automatically set by Hibernate on
   * creation and each update.
   */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /**
   * A list of posts created by this user. This defines a one-to-many relationship between User and
   * Post. {@code cascade = CascadeType.ALL} means that operations (persist, remove, etc.) on a User
   * will cascade to their Posts. {@code fetch = FetchType.LAZY} means the posts are only loaded
   * from the database when accessed.
   */
  @OneToMany(
      mappedBy = "user",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private List<Post> posts = new ArrayList<>();

  // --- UserDetails Fields ---
  private boolean enabled = true;
  private boolean accountNonExpired = true;
  private boolean accountNonLocked = true;
  private boolean credentialsNonExpired = true;

  /** Default constructor required by JPA. */
  public User() {}

  /**
   * Constructs a new User with essential details.
   *
   * @param username The user's chosen username.
   * @param email The user's email address.
   * @param password The user's raw (un-hashed) password.
   * @param role The role assigned to the user.
   */
  public User(String username, String email, String password, Role role) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  /**
   * Defines the roles or permissions for the user. This is a core part of the {@link UserDetails}
   * contract for Spring Security.
   *
   * @return A collection of authorities, prefixed with "ROLE_".
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  // --- UserDetails Contract Methods ---

  @Override
  public boolean isAccountNonExpired() {
    return accountNonExpired;
  }

  @Override
  public boolean isAccountNonLocked() {
    return accountNonLocked;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return credentialsNonExpired;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /** Enum representing the possible roles a user can have. */
  public enum Role {
    USER,
    ADMIN,
    MODERATOR
  }

  // --- Getters and Setters ---

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<Post> getPosts() {
    return posts;
  }

  public void setPosts(List<Post> posts) {
    this.posts = posts;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setAccountNonExpired(boolean accountNonExpired) {
    this.accountNonExpired = accountNonExpired;
  }

  public void setAccountNonLocked(boolean accountNonLocked) {
    this.accountNonLocked = accountNonLocked;
  }

  public void setCredentialsNonExpired(boolean credentialsNonExpired) {
    this.credentialsNonExpired = credentialsNonExpired;
  }
}
