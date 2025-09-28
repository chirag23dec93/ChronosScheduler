package com.chronos.service.impl;

import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.domain.model.payload.HttpJobPayload;
import com.chronos.exception.ResourceNotFoundException;
import com.chronos.exception.ChronosException;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunRepository;
import com.chronos.service.AuditService;
import com.chronos.service.QuartzSchedulerService;
import com.chronos.monitoring.JobMetrics;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobRunRepository jobRunRepository;

    @Mock
    private QuartzSchedulerService quartzSchedulerService;

    @Mock
    private AuditService auditService;

    @Mock
    private JobMetrics jobMetrics;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JobServiceImpl jobService;

    private User testUser;
    private Job testJob;
    private HttpJobPayload httpPayload;
    private JobSchedule jobSchedule;
    private RetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");

        // Setup HTTP payload
        httpPayload = new HttpJobPayload();
        httpPayload.setHttpUrl("https://httpbin.org/get");
        httpPayload.setHttpMethod("GET");

        // Setup job schedule
        jobSchedule = new JobSchedule();
        jobSchedule.setScheduleType(ScheduleType.ONCE);
        jobSchedule.setRunAt(Instant.now().plusSeconds(3600));
        jobSchedule.setMisfirePolicy(MisfirePolicy.FIRE_NOW);

        // Setup retry policy
        retryPolicy = new RetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryPolicy.setBackoffStrategy(BackoffStrategy.EXPONENTIAL);
        retryPolicy.setBackoffSeconds(5);

        // Setup test job
        testJob = new Job();
        testJob.setId("job-123");
        testJob.setName("Test HTTP Job");
        testJob.setType(JobType.HTTP);
        testJob.setStatus(JobStatus.PENDING);
        testJob.setPriority(JobPriority.MEDIUM);
        testJob.setOwner(testUser);
        testJob.setPayload(httpPayload);
        testJob.setSchedule(jobSchedule);
        testJob.setRetryPolicy(retryPolicy);
        testJob.setCreatedAt(Instant.now());

        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createJob_Success() {
        // Given
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(quartzSchedulerService).scheduleJob(any(Job.class));
        doNothing().when(jobMetrics).recordJobSubmission();

        // When
        Job result = jobService.createJob(testJob);

        // Then
        assertNotNull(result);
        assertEquals("Test HTTP Job", result.getName());
        assertEquals(JobType.HTTP, result.getType());
        assertEquals(JobStatus.SCHEDULED, result.getStatus());
        
        verify(jobRepository).save(any(Job.class));
        verify(quartzSchedulerService).scheduleJob(any(Job.class));
        verify(jobMetrics).recordJobSubmission();
        verify(auditService).logJobCreated(any(Job.class));
    }

    @Test
    void createJob_InvalidPayload_ThrowsException() {
        // Given
        httpPayload.setHttpUrl(null); // Invalid URL
        testJob.setPayload(httpPayload);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> jobService.createJob(testJob));
        
        verify(jobRepository, never()).save(any(Job.class));
        verify(quartzSchedulerService, never()).scheduleJob(any(Job.class));
    }

    @Test
    void getJobById_Success() {
        // Given
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        // When
        Job result = jobService.getJobById("job-123");

        // Then
        assertNotNull(result);
        assertEquals("job-123", result.getId());
        assertEquals("Test HTTP Job", result.getName());
        
        verify(jobRepository).findById("job-123");
    }

    @Test
    void getJobById_NotFound_ThrowsException() {
        // Given
        when(jobRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> jobService.getJobById("nonexistent"));
        
        verify(jobRepository).findById("nonexistent");
    }

    @Test
    void getJobById_UnauthorizedAccess_ThrowsException() {
        // Given
        User differentUser = new User();
        differentUser.setEmail("different@example.com");
        testJob.setOwner(differentUser);
        
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        // When & Then
        assertThrows(ChronosException.class, () -> jobService.getJobById("job-123"));
        
        verify(jobRepository).findById("job-123");
    }

    @Test
    void getAllJobs_Success() {
        // Given
        List<Job> jobs = List.of(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs);
        when(jobRepository.findByOwnerEmail(eq("test@example.com"), any(Pageable.class)))
            .thenReturn(jobPage);

        // When
        Page<Job> result = jobService.getAllJobs(Pageable.unpaged());

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test HTTP Job", result.getContent().get(0).getName());
        
        verify(jobRepository).findByOwnerEmail(eq("test@example.com"), any(Pageable.class));
    }

    @Test
    void pauseJob_Success() {
        // Given
        testJob.setStatus(JobStatus.SCHEDULED);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(quartzSchedulerService).pauseJob("job-123");

        // When
        Job result = jobService.pauseJob("job-123");

        // Then
        assertNotNull(result);
        assertEquals(JobStatus.PAUSED, result.getStatus());
        
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(any(Job.class));
        verify(quartzSchedulerService).pauseJob("job-123");
        verify(auditService).logJobPaused(any(Job.class));
    }

    @Test
    void pauseJob_AlreadyPaused_NoChange() {
        // Given
        testJob.setStatus(JobStatus.PAUSED);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));

        // When
        Job result = jobService.pauseJob("job-123");

        // Then
        assertNotNull(result);
        assertEquals(JobStatus.PAUSED, result.getStatus());
        
        verify(jobRepository).findById("job-123");
        verify(jobRepository, never()).save(any(Job.class));
        verify(quartzSchedulerService, never()).pauseJob(anyString());
    }

    @Test
    void resumeJob_Success() {
        // Given
        testJob.setStatus(JobStatus.PAUSED);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(quartzSchedulerService).resumeJob("job-123");

        // When
        Job result = jobService.resumeJob("job-123");

        // Then
        assertNotNull(result);
        assertEquals(JobStatus.SCHEDULED, result.getStatus());
        
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(any(Job.class));
        verify(quartzSchedulerService).resumeJob("job-123");
        verify(auditService).logJobResumed(any(Job.class));
    }

    @Test
    void cancelJob_Success() {
        // Given
        testJob.setStatus(JobStatus.SCHEDULED);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(quartzSchedulerService).deleteJob("job-123");

        // When
        Job result = jobService.cancelJob("job-123");

        // Then
        assertNotNull(result);
        assertEquals(JobStatus.CANCELLED, result.getStatus());
        
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(any(Job.class));
        verify(quartzSchedulerService).deleteJob("job-123");
        verify(auditService).logJobCancelled(any(Job.class));
    }

    @Test
    void markJobAsRunning_Success() {
        // Given
        testJob.setStatus(JobStatus.SCHEDULED);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        // When
        jobService.markJobAsRunning("job-123");

        // Then
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(argThat(job -> job.getStatus() == JobStatus.RUNNING));
    }

    @Test
    void markJobAsComplete_Success_OnceJob() {
        // Given
        testJob.setStatus(JobStatus.RUNNING);
        testJob.getSchedule().setScheduleType(ScheduleType.ONCE);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(jobMetrics).recordJobSuccess();

        // When
        jobService.markJobAsComplete("job-123", true);

        // Then
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(argThat(job -> job.getStatus() == JobStatus.SUCCEEDED));
        verify(jobMetrics).recordJobSuccess();
    }

    @Test
    void markJobAsComplete_Success_CronJob() {
        // Given
        testJob.setStatus(JobStatus.RUNNING);
        testJob.getSchedule().setScheduleType(ScheduleType.CRON);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(jobMetrics).recordJobSuccess();

        // When
        jobService.markJobAsComplete("job-123", true);

        // Then
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(argThat(job -> job.getStatus() == JobStatus.SCHEDULED));
        verify(jobMetrics).recordJobSuccess();
    }

    @Test
    void markJobAsComplete_Failure() {
        // Given
        testJob.setStatus(JobStatus.RUNNING);
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        when(jobRepository.save(any(Job.class))).thenReturn(testJob);
        doNothing().when(jobMetrics).recordJobFailure();

        // When
        jobService.markJobAsComplete("job-123", false);

        // Then
        verify(jobRepository).findById("job-123");
        verify(jobRepository).save(argThat(job -> job.getStatus() == JobStatus.FAILED));
        verify(jobMetrics).recordJobFailure();
    }

    @Test
    void deleteJob_Success() {
        // Given
        when(jobRepository.findById("job-123")).thenReturn(Optional.of(testJob));
        doNothing().when(jobRepository).delete(any(Job.class));
        doNothing().when(quartzSchedulerService).deleteJob("job-123");

        // When
        jobService.deleteJob("job-123");

        // Then
        verify(jobRepository).findById("job-123");
        verify(jobRepository).delete(testJob);
        verify(quartzSchedulerService).deleteJob("job-123");
        verify(auditService).logJobDeleted(any(Job.class));
    }
}
