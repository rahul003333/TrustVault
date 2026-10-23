
package com.trustvault.service;

import com.trustvault.entity.AuditLog;
import com.trustvault.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String username,
            String action,
            String resourceType,
            Long resourceId,
            String status,
            String details) {

        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setStatus(status);
        log.setDetails(details);

        auditLogRepository.save(log);
    }
}
