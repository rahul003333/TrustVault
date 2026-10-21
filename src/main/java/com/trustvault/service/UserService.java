
package com.trustvault.service;

import com.trustvault.entity.User;
import com.trustvault.entity.Role;
import com.trustvault.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(
            String username,
            String email,
            String rawPassword) {
        return createUser(username, email, rawPassword, Role.VIEWER);
    }

    @Transactional
    public User registerDocumentOwner(
            String username,
            String email,
            String rawPassword) {
        return createUser(
                username, email, rawPassword, Role.DOCUMENT_OWNER);
    }

    private User createUser(
            String username,
            String email,
            String rawPassword,
            Role role) {

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);

        return userRepository.save(user);
    }

    public User loginUser(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid username or password"));

        if (!passwordEncoder.matches(
                rawPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "Invalid username or password");
        }

        return user;
    }
}
