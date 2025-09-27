package com.chronos.api.controller;

import com.chronos.api.dto.audit.AuditEventResponse;
import com.chronos.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log management APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "List audit events")
    public ResponseEntity<Page<AuditEventResponse>> getAuditEvents(
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAuditEvents(pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit history for an entity")
    public ResponseEntity<Page<AuditEventResponse>> getEntityAuditEvents(
            @PathVariable String entityType,
            @PathVariable String entityId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAuditEventsForEntity(
                entityType, entityId, pageable));
    }

    @GetMapping("/action/{action}")
    @Operation(summary = "Get audit events by action type")
    public ResponseEntity<Page<AuditEventResponse>> getActionAuditEvents(
            @PathVariable String action,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAuditEventsByAction(action, pageable));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get audit events for a user")
    public ResponseEntity<Page<AuditEventResponse>> getUserAuditEvents(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditService.getAuditEventsForUser(userId, pageable));
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "Clean up old audit events")
    public ResponseEntity<Void> cleanupOldEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before
    ) {
        auditService.cleanupOldEvents(before);
        return ResponseEntity.accepted().build();
    }
}
