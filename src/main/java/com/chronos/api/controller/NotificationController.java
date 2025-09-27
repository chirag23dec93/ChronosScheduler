package com.chronos.api.controller;

import com.chronos.api.dto.notification.CreateNotificationRequest;
import com.chronos.api.dto.notification.NotificationResponse;
import com.chronos.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/jobs/{jobId}/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Job notification management APIs")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @Operation(summary = "Add notification configuration to a job")
    public ResponseEntity<NotificationResponse> addNotification(
            @PathVariable String jobId,
            @RequestBody @Valid CreateNotificationRequest request
    ) {
        NotificationResponse notification = notificationService.addNotification(jobId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/jobs/" + jobId + 
                        "/notifications/" + notification.getId()))
                .body(notification);
    }

    @GetMapping
    @Operation(summary = "List job notifications")
    public ResponseEntity<Page<NotificationResponse>> getJobNotifications(
            @PathVariable String jobId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.getJobNotifications(jobId, pageable));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification configuration")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String jobId,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotification(jobId, notificationId);
        return ResponseEntity.noContent().build();
    }
}
