
package com.trustvault.security;

import com.trustvault.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuditAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditService auditService;

    public AuditAccessDeniedHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception)
            throws IOException, ServletException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            username = authentication.getName();
        }

        String path = request.getRequestURI();
        String action = "ACCESS_DENIED";
        String resourceType = "ENDPOINT";
        Long resourceId = null;

        if (path.matches(".*/api/documents/\\d+/download")) {
            action = "DOCUMENT_DOWNLOAD";
            resourceType = "DOCUMENT";

            Matcher matcher = Pattern.compile("/api/documents/(\\d+)/download$")
                    .matcher(path);

            if (matcher.find()) {
                try {
                    resourceId = Long.parseLong(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    resourceId = null;
                }
            }
        } else if (path.endsWith("/api/documents/upload")) {
            action = "DOCUMENT_UPLOAD";
            resourceType = "DOCUMENT";
        } else if (path.endsWith("/api/documents/my")) {
            action = "DOCUMENT_LIST";
            resourceType = "DOCUMENT";
        }

        try {
            auditService.log(
                    username,
                    action,
                    resourceType,
                    resourceId,
                    "DENIED",
                    "Access denied for request path: " + path
            );
        } catch (RuntimeException auditException) {
            // Keep the original access denial response even if audit storage fails.
            // Do not expose internal audit errors to the requester.
        }

        response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied"
        );
    }
}
