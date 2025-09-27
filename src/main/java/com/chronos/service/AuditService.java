package com.chronos.service;

import com.chronos.api.dto.audit.AuditEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Map;

public interface AuditService {
    
    void auditEvent(String action, String entityType, String entityId);
    
    void auditEvent(String action, String entityType, String entityId, Map<String, Object> details);
    
    Page<AuditEventResponse> getAuditEvents(Pageable pageable);
    
    Page<AuditEventResponse> getAuditEventsForEntity(String entityType, String entityId, Pageable pageable);
    
    Page<AuditEventResponse> getAuditEventsByAction(String action, Pageable pageable);
    
    Page<AuditEventResponse> getAuditEventsForUser(Long userId, Pageable pageable);
    
    void cleanupOldEvents(Instant before);
}
