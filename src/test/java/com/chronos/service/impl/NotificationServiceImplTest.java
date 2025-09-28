package com.chronos.service.impl;

import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.repository.NotificationRepository;
import com.chronos.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private AuditService auditService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Job testJob;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");

        // Setup test job
        testJob = new Job();
        testJob.setId("job-123");
        testJob.setName("Test Job");
        testJob.setType(JobType.HTTP);
        testJob.setStatus(JobStatus.SUCCEEDED);
        testJob.setOwner(testUser);
        testJob.setCreatedAt(Instant.now());

        // Setup test notification
        testNotification = new Notification();
        testNotification.setId("notification-123");
        testNotification.setUser(testUser);
        testNotification.setJob(testJob);
        testNotification.setChannel(NotificationChannel.EMAIL);
        testNotification.setTarget("test@example.com");
        testNotification.setTemplateCode("JOB_SUCCESS");
        testNotification.setPayload(Map.of("jobName", "Test Job"));
        testNotification.setCreatedAt(Instant.now());
    }

    @Test
    void createNotification_Success() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.createNotification(testNotification);

        // Then
        assertNotNull(result);
        assertEquals("notification-123", result.getId());
        assertEquals(NotificationChannel.EMAIL, result.getChannel());
        assertEquals("test@example.com", result.getTarget());
        
        verify(notificationRepository).save(testNotification);
        verify(auditService).logNotificationCreated(testNotification);
    }

    @Test
    void getNotificationById_Success() {
        // Given
        when(notificationRepository.findById("notification-123")).thenReturn(Optional.of(testNotification));

        // When
        Notification result = notificationService.getNotificationById("notification-123");

        // Then
        assertNotNull(result);
        assertEquals("notification-123", result.getId());
        assertEquals("test@example.com", result.getTarget());
        
        verify(notificationRepository).findById("notification-123");
    }

    @Test
    void getNotificationById_NotFound_ThrowsException() {
        // Given
        when(notificationRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> notificationService.getNotificationById("nonexistent")
        );

        assertTrue(exception.getMessage().contains("Notification not found"));
        verify(notificationRepository).findById("nonexistent");
    }

    @Test
    void getAllNotifications_Success() {
        // Given
        List<Notification> notifications = List.of(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications);
        when(notificationRepository.findAll(any(Pageable.class))).thenReturn(notificationPage);

        // When
        Page<Notification> result = notificationService.getAllNotifications(Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("notification-123", result.getContent().get(0).getId());
        
        verify(notificationRepository).findAll(any(Pageable.class));
    }

    @Test
    void sendEmailNotification_Success() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("JOB_SUCCESS"), any(Context.class)))
            .thenReturn("<html><body>Job Test Job completed successfully!</body></html>");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        assertDoesNotThrow(() -> notificationService.sendEmailNotification(testNotification));

        // Then
        verify(mailSender).createMimeMessage();
        verify(templateEngine).process(eq("JOB_SUCCESS"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailNotification_TemplateProcessingError_HandlesGracefully() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("JOB_SUCCESS"), any(Context.class)))
            .thenThrow(new RuntimeException("Template processing failed"));

        // When & Then
        assertDoesNotThrow(() -> notificationService.sendEmailNotification(testNotification));
        
        verify(templateEngine).process(eq("JOB_SUCCESS"), any(Context.class));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendWebhookNotification_Success() {
        // Given
        testNotification.setChannel(NotificationChannel.WEBHOOK);
        testNotification.setTarget("https://webhook.example.com/notifications");
        
        when(restTemplate.postForObject(
            eq("https://webhook.example.com/notifications"),
            any(Map.class),
            eq(String.class)
        )).thenReturn("OK");

        // When
        assertDoesNotThrow(() -> notificationService.sendWebhookNotification(testNotification));

        // Then
        verify(restTemplate).postForObject(
            eq("https://webhook.example.com/notifications"),
            any(Map.class),
            eq(String.class)
        );
    }

    @Test
    void sendWebhookNotification_HttpError_HandlesGracefully() {
        // Given
        testNotification.setChannel(NotificationChannel.WEBHOOK);
        testNotification.setTarget("https://webhook.example.com/notifications");
        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("HTTP request failed"));

        // When & Then
        assertDoesNotThrow(() -> notificationService.sendWebhookNotification(testNotification));
        
        verify(restTemplate).postForObject(anyString(), any(), eq(String.class));
    }

    @Test
    void processNotification_EmailChannel_Success() {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("Email content");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        assertDoesNotThrow(() -> notificationService.processNotification(testNotification));

        // Then
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void processNotification_WebhookChannel_Success() {
        // Given
        testNotification.setChannel(NotificationChannel.WEBHOOK);
        testNotification.setTarget("https://webhook.example.com/notifications");
        
        when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn("OK");

        // When
        assertDoesNotThrow(() -> notificationService.processNotification(testNotification));

        // Then
        verify(restTemplate).postForObject(anyString(), any(), eq(String.class));
    }

    @Test
    void processNotification_UnsupportedChannel_HandlesGracefully() {
        // Given
        // Assuming there might be other channels in the future
        testNotification.setChannel(NotificationChannel.EMAIL); // Use existing channel for test

        // When
        assertDoesNotThrow(() -> notificationService.processNotification(testNotification));

        // Then - Should not throw exception
        verify(mailSender, atLeastOnce()).createMimeMessage();
    }

    @Test
    void sendJobCompletionNotification_Success() {
        // Given
        when(notificationRepository.findByJobId("job-123")).thenReturn(List.of(testNotification));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("Job completed successfully");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        assertDoesNotThrow(() -> notificationService.sendJobCompletionNotification(testJob));

        // Then
        verify(notificationRepository).findByJobId("job-123");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendJobFailureNotification_Success() {
        // Given
        testJob.setStatus(JobStatus.FAILED);
        testNotification.setTemplateCode("JOB_FAILURE");
        
        when(notificationRepository.findByJobId("job-123")).thenReturn(List.of(testNotification));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("JOB_FAILURE"), any(Context.class)))
            .thenReturn("Job failed");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        assertDoesNotThrow(() -> notificationService.sendJobFailureNotification(testJob, "Connection timeout"));

        // Then
        verify(notificationRepository).findByJobId("job-123");
        verify(templateEngine).process(eq("JOB_FAILURE"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void deleteNotification_Success() {
        // Given
        when(notificationRepository.findById("notification-123")).thenReturn(Optional.of(testNotification));
        doNothing().when(notificationRepository).delete(testNotification);

        // When
        assertDoesNotThrow(() -> notificationService.deleteNotification("notification-123"));

        // Then
        verify(notificationRepository).findById("notification-123");
        verify(notificationRepository).delete(testNotification);
        verify(auditService).logNotificationDeleted(testNotification);
    }

    @Test
    void updateNotificationStatus_Success() {
        // Given
        when(notificationRepository.findById("notification-123")).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        Notification result = notificationService.updateNotificationStatus("notification-123", NotificationStatus.SENT);

        // Then
        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        
        verify(notificationRepository).findById("notification-123");
        verify(notificationRepository).save(testNotification);
    }

    @Test
    void getNotificationsByUser_Success() {
        // Given
        List<Notification> notifications = List.of(testNotification);
        Page<Notification> notificationPage = new PageImpl<>(notifications);
        when(notificationRepository.findByUserId(eq("user-123"), any(Pageable.class)))
            .thenReturn(notificationPage);

        // When
        Page<Notification> result = notificationService.getNotificationsByUser("user-123", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user-123", result.getContent().get(0).getUser().getId());
        
        verify(notificationRepository).findByUserId(eq("user-123"), any(Pageable.class));
    }

    @Test
    void getNotificationsByJob_Success() {
        // Given
        List<Notification> notifications = List.of(testNotification);
        when(notificationRepository.findByJobId("job-123")).thenReturn(notifications);

        // When
        List<Notification> result = notificationService.getNotificationsByJob("job-123");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("job-123", result.get(0).getJob().getId());
        
        verify(notificationRepository).findByJobId("job-123");
    }

    @Test
    void createEmailTemplate_Success() {
        // Given
        Map<String, Object> templateData = Map.of(
            "jobName", "Test Job",
            "status", "SUCCESS",
            "duration", "5 seconds"
        );

        when(templateEngine.process(eq("JOB_SUCCESS"), any(Context.class)))
            .thenReturn("Job Test Job completed successfully in 5 seconds!");

        // When
        String result = notificationService.createEmailTemplate("JOB_SUCCESS", templateData);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Test Job"));
        assertTrue(result.contains("successfully"));
        
        verify(templateEngine).process(eq("JOB_SUCCESS"), any(Context.class));
    }

    @Test
    void batchProcessNotifications_Success() {
        // Given
        List<Notification> notifications = List.of(testNotification);
        when(notificationRepository.findPendingNotifications(any()))
            .thenReturn(notifications);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("Email content");
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        assertDoesNotThrow(() -> notificationService.batchProcessNotifications(10));

        // Then
        verify(notificationRepository).findPendingNotifications(10);
        verify(mailSender).send(mimeMessage);
    }
}
