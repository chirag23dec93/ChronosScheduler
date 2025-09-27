package com.chronos.api.controller;

import com.chronos.api.dto.dlq.DLQEventResponse;
import com.chronos.service.DLQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dlq")
@RequiredArgsConstructor
@Tag(name = "DLQ", description = "Dead Letter Queue management APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class DLQController {

    private final DLQService dlqService;

    @GetMapping
    @Operation(summary = "List DLQ events")
    public ResponseEntity<Page<DLQEventResponse>> getDLQEvents(
            @RequestParam(required = false) String reasonContains,
            Pageable pageable
    ) {
        if (reasonContains != null) {
            return ResponseEntity.ok(dlqService.searchDLQEvents(reasonContains, pageable));
        }
        return ResponseEntity.ok(dlqService.getDLQEvents(pageable));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get DLQ event details")
    public ResponseEntity<DLQEventResponse> getDLQEvent(
            @PathVariable Long eventId
    ) {
        return ResponseEntity.ok(dlqService.getDLQEvent(eventId));
    }

    @PostMapping("/{eventId}:replay")
    @Operation(summary = "Replay a failed job from DLQ")
    public ResponseEntity<Void> replayDLQEvent(
            @PathVariable Long eventId
    ) {
        dlqService.replayDLQEvent(eventId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping(":replay-all")
    @Operation(summary = "Replay all failed jobs from DLQ")
    public ResponseEntity<Void> replayAllDLQEvents() {
        dlqService.replayAllDLQEvents();
        return ResponseEntity.accepted().build();
    }
}
