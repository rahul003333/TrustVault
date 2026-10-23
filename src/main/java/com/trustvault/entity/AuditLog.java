
package com.trustvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRUSTVAULT_AUDIT_LOGS")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(
            name = "audit_log_seq",
            sequenceName = "TRUSTVAULT_AUDIT_SEQ",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "USERNAME", length = 100)
    private String username;

    @Column(name = "ACTION", nullable = false, length = 50)
    private String action;

    @Column(name = "RESOURCE_TYPE", length = 50)
    private String resourceType;

    @Column(name = "RESOURCE_ID")
    private Long resourceId;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "DETAILS", length = 1000)
    private String details;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
