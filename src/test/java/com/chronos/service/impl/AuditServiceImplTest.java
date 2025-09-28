package com.chronos.service.impl;

import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuditServiceImpl auditService;

    private User testUser;
    private Job testJob;
    private AuditEvent testAuditEvent;


    
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
        testJob.setStatus(JobStatus.SCHEDULED);
        testJob.setOwner(testUser);
        testJob.setCreatedAt(Instant.now());

        // Setup test audit event
        testAuditEvent = new AuditEvent();
        testAuditEvent.setId("audit-123");
        testAuditEvent.setEntityType("Job");
        testAuditEvent.setEntityId("job-123");
        testAuditEvent.setAction("CREATE");
        testAuditEvent.setUserEmail("test@example.com");
        testAuditEvent.setTimestamp(Instant.now());
        testAuditEvent.setDetails(Map.of("jobName", "Test Job", "jobType", "HTTP"));

        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void logJobCreated_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobCreated(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("CREATE") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logJobUpdated_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobUpdated(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("UPDATE") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logJobDeleted_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobDeleted(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("DELETE") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logJobPaused_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobPaused(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("PAUSE") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logJobResumed_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobResumed(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("RESUME") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logJobCancelled_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobCancelled(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("CANCEL") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logJobExecutionStarted_Success() {
        // Given
        JobRun jobRun = new JobRun();
        jobRun.setId("run-123");
        jobRun.setJob(testJob);
        jobRun.setAttempt(1);
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobExecutionStarted(jobRun));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("JobRun") &&
            event.getEntityId().equals("run-123") &&
            event.getAction().equals("EXECUTION_STARTED")
        ));
    }

    @Test
    void logJobExecutionCompleted_Success() {
        // Given
        JobRun jobRun = new JobRun();
        jobRun.setId("run-123");
        jobRun.setJob(testJob);
        jobRun.setAttempt(1);
        jobRun.setOutcome(JobOutcome.SUCCESS);
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobExecutionCompleted(jobRun));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("JobRun") &&
            event.getEntityId().equals("run-123") &&
            event.getAction().equals("EXECUTION_COMPLETED") &&
            event.getDetails().containsKey("outcome")
        ));
    }

    @Test
    void logUserLogin_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logUserLogin("test@example.com"));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("User") &&
            event.getAction().equals("LOGIN") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logUserLogout_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logUserLogout("test@example.com"));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("User") &&
            event.getAction().equals("LOGOUT") &&
            event.getUserEmail().equals("test@example.com")
        ));
    }

    @Test
    void logNotificationCreated_Success() {
        // Given
        Notification notification = new Notification();
        notification.setId("notification-123");
        notification.setUser(testUser);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setTarget("test@example.com");
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logNotificationCreated(notification));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Notification") &&
            event.getEntityId().equals("notification-123") &&
            event.getAction().equals("CREATE") &&
            event.getDetails().containsKey("channel")
        ));
    }

    @Test
    void logNotificationSent_Success() {
        // Given
        Notification notification = new Notification();
        notification.setId("notification-123");
        notification.setUser(testUser);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setTarget("test@example.com");
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logNotificationSent(notification));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Notification") &&
            event.getEntityId().equals("notification-123") &&
            event.getAction().equals("SENT")
        ));
    }

    @Test
    void logNotificationDeleted_Success() {
        // Given
        Notification notification = new Notification();
        notification.setId("notification-123");
        notification.setUser(testUser);
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logNotificationDeleted(notification));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Notification") &&
            event.getEntityId().equals("notification-123") &&
            event.getAction().equals("DELETE")
        ));
    }

    @Test
    void getAuditEvents_Success() {
        // Given
        List<AuditEvent> events = List.of(testAuditEvent);
        Page<AuditEvent> eventPage = new PageImpl<>(events);
        when(auditEventRepository.findAll(any(Pageable.class))).thenReturn(eventPage);

        // When
        Page<AuditEvent> result = auditService.getAuditEvents(Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("audit-123", result.getContent().get(0).getId());
        
        verify(auditEventRepository).findAll(any(Pageable.class));
    }

    @Test
    void getAuditEventsByEntity_Success() {
        // Given
        List<AuditEvent> events = List.of(testAuditEvent);
        Page<AuditEvent> eventPage = new PageImpl<>(events);
        when(auditEventRepository.findByEntityTypeAndEntityId(eq("Job"), eq("job-123"), any(Pageable.class)))
            .thenReturn(eventPage);

        // When
        Page<AuditEvent> result = auditService.getAuditEventsByEntity("Job", "job-123", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Job", result.getContent().get(0).getEntityType());
        assertEquals("job-123", result.getContent().get(0).getEntityId());
        
        verify(auditEventRepository).findByEntityTypeAndEntityId(eq("Job"), eq("job-123"), any(Pageable.class));
    }

    @Test
    void getAuditEventsByUser_Success() {
        // Given
        List<AuditEvent> events = List.of(testAuditEvent);
        Page<AuditEvent> eventPage = new PageImpl<>(events);
        when(auditEventRepository.findByUserEmail(eq("test@example.com"), any(Pageable.class)))
            .thenReturn(eventPage);

        // When
        Page<AuditEvent> result = auditService.getAuditEventsByUser("test@example.com", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test@example.com", result.getContent().get(0).getUserEmail());
        
        verify(auditEventRepository).findByUserEmail(eq("test@example.com"), any(Pageable.class));
    }

    @Test
    void getAuditEventsByAction_Success() {
        // Given
        List<AuditEvent> events = List.of(testAuditEvent);
        Page<AuditEvent> eventPage = new PageImpl<>(events);
        when(auditEventRepository.findByAction(eq("CREATE"), any(Pageable.class)))
            .thenReturn(eventPage);

        // When
        Page<AuditEvent> result = auditService.getAuditEventsByAction("CREATE", Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("CREATE", result.getContent().get(0).getAction());
        
        verify(auditEventRepository).findByAction(eq("CREATE"), any(Pageable.class));
    }

    @Test
    void getAuditEventById_Success() {
        // Given
        when(auditEventRepository.findById("audit-123")).thenReturn(Optional.of(testAuditEvent));

        // When
        AuditEvent result = auditService.getAuditEventById("audit-123");

        // Then
        assertNotNull(result);
        assertEquals("audit-123", result.getId());
        assertEquals("Job", result.getEntityType());
        
        verify(auditEventRepository).findById("audit-123");
    }

    @Test
    void getAuditEventById_NotFound_ThrowsException() {
        // Given
        when(auditEventRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> auditService.getAuditEventById("nonexistent")
        );

        assertTrue(exception.getMessage().contains("Audit event not found"));
        verify(auditEventRepository).findById("nonexistent");
    }

    @Test
    void cleanupOldAuditEvents_Success() {
        // Given
        Instant cutoffDate = Instant.now().minusSeconds(86400 * 30); // 30 days ago
        when(auditEventRepository.deleteByTimestampBefore(any(Instant.class))).thenReturn(5L);

        // When
        long deletedCount = auditService.cleanupOldAuditEvents(30);

        // Then
        assertEquals(5L, deletedCount);
        verify(auditEventRepository).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    void logWithoutAuthentication_HandlesGracefully() {
        // Given
        SecurityContextHolder.clearContext(); // No authentication context
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logJobCreated(testJob));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("Job") &&
            event.getEntityId().equals("job-123") &&
            event.getAction().equals("CREATE") &&
            (event.getUserEmail() == null || event.getUserEmail().equals("system"))
        ));
    }

    @Test
    void logCustomEvent_Success() {
        // Given
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(testAuditEvent);

        // When
        assertDoesNotThrow(() -> auditService.logCustomEvent(
            "CustomEntity", 
            "entity-123", 
            "CUSTOM_ACTION", 
            Map.of("key", "value")
        ));

        // Then
        verify(auditEventRepository).save(argThat(event -> 
            event.getEntityType().equals("CustomEntity") &&
            event.getEntityId().equals("entity-123") &&
            event.getAction().equals("CUSTOM_ACTION") &&
            event.getDetails().containsKey("key")
        ));
    }
}
