package com.example.graphql.service;

import com.example.graphql.entity.User;
import com.example.graphql.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class UserService implements ReactiveUserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return findByUsernameReactive(username)
                .switchIfEmpty(Mono.defer(() -> {
                    String errorMessage = "User not found: " + username;
                    logger.warn(errorMessage);
                    return Mono.error(new UsernameNotFoundException(errorMessage));
                }))
                .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().name().replace("ROLE_", ""))
                        .build());
    }

    /** Reactive wrapper for blocking JPA call */
    public Mono<User> findByUsernameReactive(String username) {
        return Mono.fromCallable(() -> userRepository.findByUsername(username).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public User getUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
    }

    public List<User> searchUsers(String search) {
        // Return first 100 results for simplicity
        return userRepository
                .searchUsers(search, org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent();
    }

    public long countUsers() {
        return userRepository.count();
    }

    public User createUser(String username, String email, String password, String role) {
        User.Role userRole;
        try {
            userRole = User.Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        User user = new User(username, email, password, userRole);
        return userRepository.save(user);
    }

    public User updateUserGraphQL(Long id, String username, String email, String password, String role) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
        if (username != null) user.setUsername(username);
        if (email != null) user.setEmail(email);
        if (password != null) user.setPassword(password);
        if (role != null) user.setRole(User.Role.valueOf(role));
        return userRepository.save(user);
    }

    public boolean deleteUserGraphQL(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UsernameNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
        return true;
    }

    public UserDetails loadUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
