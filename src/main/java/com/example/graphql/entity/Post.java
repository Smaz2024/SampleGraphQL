package com.example.graphql.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Represents a Post entity in the application, mapping to the 'posts' table in the database. This
 * entity stores information about a user's post, including its title, content, and the author. It
 * also includes timestamps for creation and last update, managed automatically by Hibernate.
 */
@Entity
@Table(name = "posts")
public class Post {

  /** The unique identifier for the post. Generated automatically using identity strategy. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The title of the post. Must not be blank and its size must be between 3 and 200 characters. */
  @NotBlank
  @Size(min = 3, max = 200)
  private String title;

  /**
   * The main content of the post. Must not be blank and its size must be between 10 and 5000
   * characters.
   */
  @NotBlank
  @Size(min = 10, max = 5000)
  private String content;

  /**
   * The user who authored this post. This is a many-to-one relationship, meaning many posts can
   * belong to one user. {@code FetchType.LAZY} is used to load the user only when accessed. {@code
   * optional = false} indicates that a post must always have an associated user.
   * {@code @JoinColumn} specifies the foreign key column in the 'posts' table.
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  /** The timestamp when the post was created. Automatically set upon creation and not updatable. */
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  /**
   * The timestamp when the post was last updated. Automatically updated whenever the entity is
   * modified.
   */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  /** Default constructor for JPA. */
  public Post() {}

  /**
   * Constructs a new Post with the specified title, content, and associated user.
   *
   * @param title The title of the post.
   * @param content The content of the post.
   * @param user The user who created the post.
   */
  public Post(String title, String content, User user) {
    this.title = title;
    this.content = content;
    this.user = user;
  }

  // --- Getters ---

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public User getUser() {
    return user;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  // Setters for mutable fields

  public void setTitle(String title) {
    this.title = title;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
