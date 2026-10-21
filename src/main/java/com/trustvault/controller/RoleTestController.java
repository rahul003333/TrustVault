
package com.trustvault.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/roles")
public class RoleTestController {

    @GetMapping("/viewer")
    @PreAuthorize("hasRole('VIEWER')")
    public String viewerAccess() {
        return "Viewer access granted";
    }

    @GetMapping("/administrator")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public String administratorAccess() {
        return "Administrator access granted";
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('DOCUMENT_OWNER')")
    public String ownerAccess() {
        return "Document owner access granted";
    }
}
