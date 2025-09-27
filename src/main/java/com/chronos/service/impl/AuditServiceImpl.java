package com.chronos.service.impl;

import com.chronos.api.dto.audit.AuditEventResponse;
import com.chronos.api.mapper.AuditEventMapper;
import com.chronos.domain.model.AuditEvent;
import com.chronos.domain.model.User;
import com.chronos.repository.AuditEventRepository;
import com.chronos.repository.UserRepository;
import com.chronos.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final AuditEventMapper auditEventMapper;

    @Override
    @Async
    @Transactional
    public void auditEvent(String action, String entityType, String entityId) {
        auditEvent(action, entityType, entityId, null);
    }

    @Override
    @Async
    @Transactional
    public void auditEvent(String action, String entityType, String entityId, Map<String, Object> details) {
        try {
            User user = getCurrentUser();
            
            AuditEvent event = AuditEvent.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .createdAt(Instant.now())
                    .details(details)
                    .build();
            
            auditEventRepository.save(event);
            log.debug("Audit event created: {} {} {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit event: {} {} {}", action, entityType, entityId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getAuditEvents(Pageable pageable) {
        return auditEventRepository.findAll(pageable)
                .map(auditEventMapper::toAuditEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getAuditEventsForEntity(String entityType, String entityId, 
                                                          Pageable pageable) {
        return auditEventRepository.findByEntity(entityType, entityId, pageable)
                .map(auditEventMapper::toAuditEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getAuditEventsByAction(String action, Pageable pageable) {
        return auditEventRepository.findByAction(action, pageable)
                .map(auditEventMapper::toAuditEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> getAuditEventsForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        return auditEventRepository.findByUser(user, pageable)
                .map(auditEventMapper::toAuditEventResponse);
    }

    @Override
    @Transactional
    public void cleanupOldEvents(Instant before) {
        try {
            auditEventRepository.deleteByCreatedAtBefore(before);
            log.info("Cleaned up audit events before {}", before);
        } catch (Exception e) {
            log.error("Failed to cleanup old audit events", e);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
