package com.trustvault.controller;

import com.trustvault.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SecurityExceptionHandler {

    private final AuditService auditService;

    public SecurityExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = null;

        if (authentication != null && authentication.isAuthenticated()) {
            username = authentication.getName();
        }

        try {
            auditService.log(
                    username,
                    "ACCESS_DENIED",
                    "ENDPOINT",
                    null,
                    "DENIED",
                    "Access denied for request path: " + request.getRequestURI()
            );
        } catch (RuntimeException ignored) {
            // Preserve the 403 response even if audit logging fails.
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied"));
    }
}