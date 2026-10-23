
package com.trustvault.controller;

import com.trustvault.dto.LoginRequest;
import com.trustvault.dto.LoginResponse;
import com.trustvault.dto.RegisterRequest;
import com.trustvault.dto.RegisterResponse;
import com.trustvault.entity.User;
import com.trustvault.security.JwtService;
import com.trustvault.service.AuditService;
import com.trustvault.service.UserService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthController(
            UserService userService,
            JwtService jwtService,
            AuditService auditService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        auditService.log(
                user.getUsername(),
                "USER_REGISTERED",
                "USER",
                user.getId(),
                "SUCCESS",
                "Viewer account registered"
        );

        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        User user;

        try {
            user = userService.loginUser(
                    request.getUsername(),
                    request.getPassword()
            );
        } catch (IllegalArgumentException exception) {
            auditService.log(
                    request.getUsername(),
                    "LOGIN",
                    "AUTHENTICATION",
                    null,
                    "FAILURE",
                    "Invalid username or password"
            );

            throw exception;
        }

        String token = jwtService.generateToken(user.getUsername());

        auditService.log(
                user.getUsername(),
                "LOGIN",
                "AUTHENTICATION",
                user.getId(),
                "SUCCESS",
                "Login successful"
        );

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                "Login successful",
                token
        );
    }

    @PostMapping("/register-owner")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse registerOwner(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.registerDocumentOwner(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );

        auditService.log(
                user.getUsername(),
                "USER_REGISTERED",
                "USER",
                user.getId(),
                "SUCCESS",
                "Document Owner account registered"
        );

        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
