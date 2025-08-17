package com.example.graphql.service;

import com.example.graphql.dto.UserUpdateDTO;
import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @CircuitBreaker(name = "userService", fallbackMethod = "loadUserFallback")
  @Retry(name = "userService")
  public UserDetails loadUserByUsername(String username) {
    log.debug("Attempting to load user by username: {}", username);
    return userRepository
        .findByUsername(username)
        .orElseThrow(
            () -> new UsernameNotFoundException("User not found with username: " + username));
  }

  public UserDetails loadUserFallback(String username, Throwable ex) {
    log.warn(
        "Circuit breaker fallback for loadUserByUsername for user '{}'. Reason: {}",
        username,
        ex.getMessage());
    throw new UsernameNotFoundException(
        "User service is temporarily unavailable. Please try again later.");
  }

  @Cacheable(cacheNames = "users", key = "#id")
  @CircuitBreaker(name = "userService", fallbackMethod = "findByIdFallback")
  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  public Optional<User> findByIdFallback(Long id, Throwable ex) {
    log.warn("Circuit breaker fallback for findById for ID '{}'. Reason: {}", id, ex.getMessage());
    return Optional.empty();
  }

  @Cacheable(cacheNames = "users", key = "#username")
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  @Cacheable(cacheNames = "users", key = "#email")
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Cacheable("allUsers")
  @CircuitBreaker(name = "userService", fallbackMethod = "findAllFallback")
  @Retry(name = "userService")
  public List<User> findAll() {
    return userRepository.findAll();
  }

  public List<User> findAllFallback(Throwable ex) {
    log.warn("Circuit breaker fallback for findAll. Reason: {}", ex.getMessage());
    return List.of();
  }

  @Caching(
      evict = {
        @CacheEvict(cacheNames = "users", allEntries = true),
        @CacheEvict(cacheNames = "allUsers", allEntries = true)
      })
  public User createUser(User user) {
    if (userRepository.existsByUsername(user.getUsername())) {
      throw new IllegalArgumentException("Username already exists: " + user.getUsername());
    }
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new IllegalArgumentException("Email already exists: " + user.getEmail());
    }
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
  }

  @Caching(
      evict = {
        @CacheEvict(cacheNames = "users", key = "#id"),
        @CacheEvict(cacheNames = "users", key = "#result.username"),
        @CacheEvict(cacheNames = "users", key = "#result.email"),
        @CacheEvict(cacheNames = "allUsers", allEntries = true)
      })
  public User updateUser(Long id, UserUpdateDTO updateDto) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

    updateDto
        .getName()
        .ifPresent(
            newName -> {
              if (userRepository.existsByUsername(newName) && !user.getUsername().equals(newName)) {
                throw new IllegalArgumentException("Username already exists: " + newName);
              }
              user.setUsername(newName);
            });

    updateDto
        .getEmail()
        .ifPresent(
            newEmail -> {
              if (userRepository.existsByEmail(newEmail) && !user.getEmail().equals(newEmail)) {
                throw new IllegalArgumentException("Email already exists: " + newEmail);
              }
              user.setEmail(newEmail);
            });

    return userRepository.save(user);
  }

  @Caching(
      evict = {
        @CacheEvict(cacheNames = "users", allEntries = true),
        @CacheEvict(cacheNames = "allUsers", allEntries = true)
      })
  public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      throw new EntityNotFoundException("User not found with ID: " + id);
    }
    userRepository.deleteById(id);
  }

  public List<User> searchUsers(String search) {
    return userRepository.searchUsers(search);
  }

  public List<User> findByRole(User.Role role) {
    return userRepository.findByRole(role);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }
}
