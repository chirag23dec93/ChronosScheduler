package com.chronos.service.impl;

import com.chronos.api.dto.job.*;
import com.chronos.api.mapper.JobMapper;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.RetryPolicy;
import com.chronos.domain.model.User;
import com.chronos.domain.model.enums.JobOutcome;
import com.chronos.domain.model.enums.JobStatus;
import com.chronos.domain.model.enums.JobType;
import com.chronos.domain.model.enums.ScheduleType;
import com.chronos.domain.model.payload.*;
import com.chronos.api.dto.job.payload.DatabaseJobPayloadDto;
import com.chronos.api.dto.job.payload.HttpJobPayloadDto;
import com.chronos.api.dto.job.payload.ScriptJobPayloadDto;
import com.chronos.api.dto.job.payload.CacheJobPayloadDto;
import com.chronos.api.dto.job.payload.MessageQueueJobPayloadDto;
import com.chronos.api.dto.job.payload.FileSystemJobPayloadDto;
import com.chronos.api.dto.job.payload.DbToKafkaJobPayloadDto;
import com.chronos.exception.InvalidJobConfigurationException;
import com.chronos.exception.JobExecutionException;
import com.chronos.exception.ResourceNotFoundException;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunLogRepository;
import com.chronos.repository.JobRunRepository;
import com.chronos.repository.UserRepository;
import com.chronos.service.AuditService;
import com.chronos.service.JobService;
import com.chronos.service.NotificationService;
import com.chronos.service.QuartzSchedulerService;
import com.chronos.monitoring.JobMetrics;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobRunRepository jobRunRepository;
    private final JobRunLogRepository jobRunLogRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;
    private final QuartzSchedulerService quartzSchedulerService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final JobMetrics jobMetrics;

    public JobServiceImpl(JobRepository jobRepository,
                         JobRunRepository jobRunRepository,
                         JobRunLogRepository jobRunLogRepository,
                         UserRepository userRepository,
                         JobMapper jobMapper,
                         QuartzSchedulerService quartzSchedulerService,
                         NotificationService notificationService,
                         AuditService auditService,
                         ApplicationEventPublisher eventPublisher,
                         JobMetrics jobMetrics) {
        this.jobRepository = jobRepository;
        this.jobRunRepository = jobRunRepository;
        this.jobRunLogRepository = jobRunLogRepository;
        this.userRepository = userRepository;
        this.jobMapper = jobMapper;
        this.quartzSchedulerService = quartzSchedulerService;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.jobMetrics = jobMetrics;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public JobResponse createJob(CreateJobRequest request) {
        log.warn("========== JOB CREATION START ==========");
        log.warn("Creating job with name: {}", request.getName());
        
        // Get current user
        User owner = getCurrentUser();
        log.warn("Found owner: {}", owner.getEmail());
        
        // Validate request
        validateJobRequest(request);
        log.warn("Request validation passed");

        // Create job
        Job job = jobMapper.toJob(request);
        job.setId(UlidCreator.getUlid().toString());
        job.setOwner(owner);
        job.setStatus(JobStatus.PENDING);
        log.warn("Created job with ID: {} and owner: {}", job.getId(), owner.getEmail());
        
        // Record job submission metrics
        jobMetrics.recordJobSubmission();
        
        // Set up relationships
        if (job.getSchedule() != null) {
            job.getSchedule().setJob(job);
            log.warn("Schedule relationship set: {}", job.getSchedule());
        }
        if (job.getPayload() != null) {
            job.getPayload().setJob(job);
            log.warn("Payload relationship set: {}", job.getPayload());
        }
        if (job.getRetryPolicy() != null) {
            job.getRetryPolicy().setJob(job);
            log.warn("Retry policy relationship set: {}", job.getRetryPolicy());
        }

        // Save job
        log.warn("Saving job to database");
        job = jobRepository.save(job);
        log.warn("Job saved successfully: {}", job);
        
        // Schedule job
        try {
            log.warn("Scheduling job with Quartz");
            quartzSchedulerService.scheduleJob(job);
            job.setStatus(JobStatus.SCHEDULED);
            job = jobRepository.save(job);
            log.warn("Job scheduled successfully: {}", job);
        } catch (SchedulerException e) {
            log.error("Failed to schedule job: {}", e.getMessage(), e);
            throw new JobExecutionException("Failed to schedule job: " + e.getMessage(), e);
        }

        // Create audit event
        log.warn("Creating audit event");
        auditService.auditEvent("JOB_CREATED", "Job", job.getId());
        log.warn("Audit event created");

        // Send notifications in a separate transaction
        log.warn("========== NOTIFICATION START ==========");
        log.warn("About to send job creation notification for job {} ({}) to owner {} and admin", 
            job.getName(), job.getId(), job.getOwner().getEmail());
        try {
            markJobCreated(job);
            log.warn("Successfully sent job creation notification for job {} ({})", job.getName(), job.getId());
        } catch (Exception e) {
            log.error("Failed to send job creation notification for job {} ({}): {}", 
                job.getName(), job.getId(), e.getMessage(), e);
            log.error("Notification error details:", e);
        }
        log.warn("========== NOTIFICATION END ==========");

        log.warn("========== JOB CREATION END ==========");
        return jobMapper.toJobResponse(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public synchronized void markJobCreated(Job initialJob) {
        try {
            // Get a fresh copy of the job in the new transaction
            Job job = jobRepository.findById(initialJob.getId())
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Job", initialJob.getId()));
            
            log.warn("Sending job creation notification for job {} ({})", job.getName(), job.getId());
            notificationService.notifyJobCreation(job);
        } catch (Exception e) {
            log.error("Error sending job creation notification for job {}: {}", initialJob.getId(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(String jobId) {
        Job job = findJobOrThrow(jobId);
        return jobMapper.toJobResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getJobs(JobStatus status, String nameContains, 
                                   Instant nextRunFrom, Instant nextRunTo, 
                                   Pageable pageable) {
        User owner = getCurrentUser();
        Page<Job> jobs;

        if (status != null) {
            jobs = jobRepository.findByOwnerAndStatus(owner, status, pageable);
        } else if (nameContains != null) {
            jobs = jobRepository.findByOwnerAndNameContaining(owner, nameContains, pageable);
        } else if (nextRunFrom != null && nextRunTo != null) {
            jobs = jobRepository.findByOwnerAndNextRunBetween(owner, nextRunFrom, nextRunTo, pageable);
        } else {
            jobs = jobRepository.findByOwner(owner, pageable);
        }

        return jobs.map(jobMapper::toJobResponse);
    }

    @Override
    @Transactional
    public JobResponse updateJob(String jobId, CreateJobRequest request) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);
        validateJobRequest(request);

        if (job.getStatus() == JobStatus.RUNNING) {
            throw new InvalidJobConfigurationException("Cannot update a running job");
        }

        try {
            quartzSchedulerService.deleteJob(job.getId());
            
            // Update job fields
            job.setName(request.getName());
            job.setType(request.getType());
            job.setPriority(request.getPriority());
            
            // Update schedule
            if (request.getSchedule() != null) {
                job.getSchedule().setScheduleType(request.getSchedule().getScheduleType());
                job.getSchedule().setRunAt(request.getSchedule().getRunAt());
                job.getSchedule().setCronExpression(request.getSchedule().getCronExpression());
                job.getSchedule().setIntervalSeconds(request.getSchedule().getIntervalSeconds());
                job.getSchedule().setTimezone(request.getSchedule().getTimezone());
                job.getSchedule().setMisfirePolicy(request.getSchedule().getMisfirePolicy());
            }
            
            // Update payload if present
            if (request.getPayload() != null) {
                JobPayloadDto payloadDto = request.getPayload();
                if (payloadDto instanceof HttpJobPayloadDto httpPayload) {
                    if (!(job.getPayload() instanceof HttpJobPayload)) {
                        throw new InvalidJobConfigurationException("Cannot change payload type from " + 
                            job.getPayload().getClass().getSimpleName() + " to HttpJobPayload");
                    }
                    HttpJobPayload existingPayload = (HttpJobPayload) job.getPayload();
                    existingPayload.setHttpUrl(httpPayload.getHttpUrl());
                    existingPayload.setHttpMethod(httpPayload.getHttpMethod());
                    existingPayload.setHttpHeaders(httpPayload.getHttpHeaders());
                    existingPayload.setHttpBody(httpPayload.getHttpBody());
                    existingPayload.setMetadata(httpPayload.getMetadata());
                } else if (payloadDto instanceof ScriptJobPayloadDto scriptPayload) {
                    if (!(job.getPayload() instanceof ScriptJobPayload)) {
                        throw new InvalidJobConfigurationException("Cannot change payload type from " + 
                            job.getPayload().getClass().getSimpleName() + " to ScriptJobPayload");
                    }
                    ScriptJobPayload existingPayload = (ScriptJobPayload) job.getPayload();
                    existingPayload.setScript(scriptPayload.getScript());
                    existingPayload.setMetadata(scriptPayload.getMetadata());
                } else {
                    throw new InvalidJobConfigurationException("Unsupported payload type: " + 
                        payloadDto.getClass().getName());
                }
            }
            
            // Update retry policy
            if (request.getRetryPolicy() != null) {
                job.getRetryPolicy().setMaxAttempts(request.getRetryPolicy().getMaxAttempts());
                job.getRetryPolicy().setBackoffStrategy(request.getRetryPolicy().getBackoffStrategy());
                job.getRetryPolicy().setBackoffSeconds(request.getRetryPolicy().getBackoffSeconds());
                job.getRetryPolicy().setRetryOn(Arrays.asList(request.getRetryPolicy().getRetryOn()));
            }

            job = jobRepository.save(job);
            quartzSchedulerService.deleteJob(job.getId());
            
            auditService.auditEvent("JOB_UPDATED", "Job", job.getId());
            return jobMapper.toJobResponse(job);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Failed to update job schedule: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteJob(String jobId) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);

        if (job.getStatus() == JobStatus.RUNNING) {
            throw new InvalidJobConfigurationException("Cannot delete a running job");
        }

        try {
            quartzSchedulerService.deleteJob(job.getId());
            jobRepository.delete(job);
            auditService.auditEvent("JOB_DELETED", "Job", jobId);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Failed to delete job schedule: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public JobResponse scheduleJob(String jobId, Instant runAt) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);

        try {
            job.getSchedule().setRunAt(runAt);
            job = jobRepository.save(job);
            quartzSchedulerService.rescheduleJob(job, runAt);
            
            auditService.auditEvent("JOB_RESCHEDULED", "Job", jobId);
            return jobMapper.toJobResponse(job);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Failed to reschedule job: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public JobResponse pauseJob(String jobId) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);

        try {
            quartzSchedulerService.pauseJob(job.getId());
            job.setStatus(JobStatus.PAUSED);
            job = jobRepository.save(job);
            
            auditService.auditEvent("JOB_PAUSED", "Job", jobId);
            return jobMapper.toJobResponse(job);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Failed to pause job: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public JobResponse resumeJob(String jobId) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);

        try {
            quartzSchedulerService.resumeJob(job.getId());
            job.setStatus(JobStatus.SCHEDULED);
            job = jobRepository.save(job);
            
            auditService.auditEvent("JOB_RESUMED", "Job", jobId);
            return jobMapper.toJobResponse(job);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Failed to resume job: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void updateAllJobStatus(JobStatus status) {
        log.info("Updating all jobs to status {}", status);
        jobRepository.updateAllJobStatus(status);
        log.info("Updated all jobs to status {}", status);
    }

    @Override
    @Transactional
    public JobResponse cancelJob(String jobId) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);

        // Update status atomically
        int updated = jobRepository.updateJobStatus(jobId, JobStatus.CANCELLED);
        if (updated == 0) {
            throw new JobExecutionException("Failed to cancel job: status update failed");
        }
        
        // Get updated job
        job = jobRepository.findById(jobId).orElseThrow();
        if (job.getStatus() != JobStatus.CANCELLED) {
            throw new JobExecutionException("Failed to cancel job: status verification failed");
        }
        
        // Delete from Quartz after status update
        try {
            quartzSchedulerService.deleteJob(jobId);
            log.info("Successfully deleted job {} from Quartz", jobId);
        } catch (SchedulerException e) {
            log.error("Failed to delete job from Quartz: {}", e.getMessage(), e);
            // Don't throw - job is already cancelled in database
        }
        
        auditService.auditEvent("JOB_CANCELLED", "Job", jobId);
        return jobMapper.toJobResponse(job);
    }

    @Override
    @Transactional
    public JobResponse triggerJobNow(String jobId) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);

        try {
            quartzSchedulerService.triggerJobNow(job.getId());
            auditService.auditEvent("JOB_TRIGGERED", "Job", jobId);
            return jobMapper.toJobResponse(job);
        } catch (SchedulerException e) {
            throw new JobExecutionException("Failed to trigger job: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobRunSummaryDto> getJobRuns(String jobId, Pageable pageable) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);
        
        Page<JobRun> runs = jobRunRepository.findByJob(job, pageable);
        return runs.map(jobMapper::toJobRunSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public JobRunSummaryDto getJobRun(String jobId, String runId) {
        Job job = findJobOrThrow(jobId);
        validateJobOwnership(job);
        
        JobRun run = jobRunRepository.findById(runId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("JobRun", runId));
        
        if (!run.getJob().getId().equals(jobId)) {
            throw new InvalidJobConfigurationException("Run does not belong to the specified job");
        }
        
        return jobMapper.toJobRunSummaryDto(run);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobRunLogDto> getJobRunLogs(String jobId, String runId, Pageable pageable) {
        JobRun run = jobRunRepository.findById(runId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("JobRun", runId));
        
        if (!run.getJob().getId().equals(jobId)) {
            throw new InvalidJobConfigurationException("Run does not belong to the specified job");
        }
        
        validateJobOwnership(run.getJob());
        
        return jobRunLogRepository.findByRun(run, pageable)
                .map(log -> {
                    JobRunLogDto dto = new JobRunLogDto();
                    dto.setId(log.getId());
                    dto.setRunId(log.getRun().getId());
                    dto.setTimestamp(log.getTimestamp());
                    dto.setLevel(log.getLevel());
                    dto.setMessage(log.getMessage());
                    dto.setContext(log.getContext());
                    return dto;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> findReadyJobs(int limit) {
        return jobRepository.findReadyToRun(JobStatus.SCHEDULED, Instant.now(), 
                Pageable.ofSize(limit))
                .stream()
                .map(jobMapper::toJobResponse)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobAsRunning(String jobId, String runId, String workerId) {
    // Lock the job first
    Job dbJob = jobRepository.findById(jobId)
            .orElseThrow(() -> ResourceNotFoundException.forResource("Job", jobId));

    // Check if job is in a valid state to run
    if (dbJob.getStatus() != JobStatus.SCHEDULED) {
        throw new JobExecutionException("Job " + jobId + " is not in SCHEDULED state (current state: " + dbJob.getStatus() + ")");
    }

    // Get attempt count from the latest run
    int attempt = jobRunRepository.findFirstByJobIdOrderByStartTimeDesc(jobId)
            .map(lastRun -> lastRun.getAttempt() + 1)
            .orElse(1);

    // Create new JobRun first
    JobRun run = JobRun.builder()
            .id(runId)
            .job(dbJob)
            .scheduledTime(Instant.now())
            .startTime(Instant.now())
            .workerId(workerId)
            .attempt(attempt)
            .build();

    jobRunRepository.saveAndFlush(run);

    try {
        // All jobs should be marked as RUNNING during execution
        dbJob.setStatus(JobStatus.RUNNING);
        jobRepository.saveAndFlush(dbJob);
    } catch (Exception e) {
        log.error("Failed to mark job {} as running: {}", jobId, e.getMessage());

        // Mark the run as failed
        run.setEndTime(Instant.now());
        run.setOutcome(JobOutcome.FAILURE);
        run.setErrorMessage("Failed to start job execution");
        run.setDurationMs(run.getEndTime().toEpochMilli() - run.getStartTime().toEpochMilli());
        jobRunRepository.saveAndFlush(run);
        
        // Mark the job as failed
        dbJob.setStatus(JobStatus.FAILED);
        jobRepository.saveAndFlush(dbJob);
        notificationService.notifyJobFailure(dbJob, run, "Failed to start job execution");
        throw e;
    }
}

@Override
public Job findJobOrThrow(String jobId) {
    return jobRepository.findById(jobId)
            .orElseThrow(() -> ResourceNotFoundException.forResource("Job", jobId));
}

@Override
public boolean shouldRetry(String jobId, int currentAttempt) {
    Job job = findJobOrThrow(jobId);
    RetryPolicy policy = job.getRetryPolicy();
    
    // Check if we haven't exceeded max attempts
    if (currentAttempt >= policy.getMaxAttempts()) {
        return false;
    }
    
    // If no specific retry conditions are configured, retry for any error
    if (policy.getRetryOn() == null || policy.getRetryOn().isEmpty()) {
        return true;
    }
    
    // Check if any of the configured retry conditions match
    // This is a simplified approach - in a real implementation, you'd want to 
    // match the actual error type from the exception to the retry conditions
    for (String retryCondition : policy.getRetryOn()) {
        switch (retryCondition.toUpperCase()) {
            case "CONNECTION_ERROR":
            case "TIMEOUT":
            case "IO_ERROR":
            case "DATABASE_ERROR":
            case "SERVER_ERROR":
            case "5XX":
            case "NETWORK_ERROR":
                return true;
            default:
                // Continue checking other conditions
                break;
        }
    }
    
    return false;
}

@Override
public Instant calculateNextRetryTime(String jobId, int currentAttempt) {
    Job job = findJobOrThrow(jobId);
    int backoffSeconds = job.getRetryPolicy().getBackoffSeconds();
    
    switch (job.getRetryPolicy().getBackoffStrategy()) {
        case FIXED:
            return Instant.now().plusSeconds(backoffSeconds);
        case EXPONENTIAL:
            long exponentialBackoff = backoffSeconds * (long) Math.pow(2, currentAttempt - 1);
            return Instant.now().plusSeconds(exponentialBackoff);
        default:
            throw new IllegalStateException("Unknown backoff strategy");
    }
}

@Override
@Transactional(isolation = Isolation.READ_COMMITTED)
public void markJobAsComplete(String jobId, String runId, boolean success, String errorMessage) {
    try {
        Job job = findJobOrThrow(jobId);
        JobRun run = jobRunRepository.findByIdAndJobId(runId, jobId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("JobRun", runId));

        run.setEndTime(Instant.now());
        run.setOutcome(success ? JobOutcome.SUCCESS : JobOutcome.FAILURE);
        if (errorMessage != null) {
            run.setErrorMessage(errorMessage);
        }
        jobRunRepository.saveAndFlush(run);

        if (success) {
            // Record job success metrics
            jobMetrics.recordJobSuccess();
            
            // For recurring jobs (CRON/INTERVAL), reset to SCHEDULED state for next execution
            // Only one-time jobs (ONCE) should be marked as SUCCEEDED
            if (job.getSchedule().getScheduleType() == ScheduleType.ONCE) {
                job.setStatus(JobStatus.SUCCEEDED);
            } else {
                // Reset CRON and INTERVAL jobs to SCHEDULED state
                job.setStatus(JobStatus.SCHEDULED);
            }
            jobRepository.saveAndFlush(job);
            notificationService.notifyJobCompletion(job, run);
        } else {
            // Record job failure metrics
            jobMetrics.recordJobFailure();
            
            // Always handle failure, regardless of current state
            handleJobFailure(job, run, errorMessage);
        }
    } catch (Exception e) {
        log.error("Error completing job {} run {}: {}", jobId, runId, e.getMessage());
        // Try one more time with a clean transaction
        try {
            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> ResourceNotFoundException.forResource("Job", jobId));
            job.setStatus(JobStatus.FAILED);
            jobRepository.saveAndFlush(job);
        } catch (Exception retryEx) {
            log.error("Failed to mark job {} as failed after error: {}", jobId, retryEx.getMessage());
        }
        throw e;
    }
}

private User getCurrentUser() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByEmail(email)
            .orElseThrow(() -> new AccessDeniedException("User not found: " + email));
}

private void validateJobOwnership(Job job) {
    User currentUser = getCurrentUser();
    if (!job.getOwner().getId().equals(currentUser.getId())) {
        throw new AccessDeniedException("User " + currentUser.getEmail() + " does not have access to job " + job.getId());
    }
}

private void validateJobRequest(CreateJobRequest request) {
    if (request.getSchedule() == null) {
        throw new InvalidJobConfigurationException("Job schedule is required");
    }
    
    if (request.getName() == null || request.getName().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Job name is required");
    }
    
    if (request.getType() == null) {
        throw new InvalidJobConfigurationException("Job type is required");
    }
    
    // Validate schedule
    if (request.getSchedule().getScheduleType() == null) {
        throw new InvalidJobConfigurationException("Schedule type is required");
    }
    
    // Validate payload based on job type
    validateJobPayload(request);
}

private void validateJobPayload(CreateJobRequest request) {
    if (request.getPayload() == null) {
        throw new InvalidJobConfigurationException("Job payload is required");
    }

    switch (request.getType()) {
        case HTTP:
            if (!(request.getPayload() instanceof HttpJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for HTTP job");
            }
            validateHttpJobPayload((HttpJobPayloadDto) request.getPayload());
            break;
        case SCRIPT:
            if (!(request.getPayload() instanceof ScriptJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for SCRIPT job");
            }
            validateScriptJobPayload((ScriptJobPayloadDto) request.getPayload());
            break;
        case CACHE:
            if (!(request.getPayload() instanceof CacheJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for CACHE job");
            }
            validateCacheJobPayload((CacheJobPayloadDto) request.getPayload());
            break;
        case MESSAGE_QUEUE:
            if (!(request.getPayload() instanceof MessageQueueJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for MESSAGE_QUEUE job");
            }
            validateMessageQueueJobPayload((MessageQueueJobPayloadDto) request.getPayload());
            break;
        case DUMMY:
            // No validation needed for dummy jobs
            break;
        case DATABASE:
            if (!(request.getPayload() instanceof DatabaseJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for DATABASE job");
            }
            validateDatabaseJobPayload((DatabaseJobPayloadDto) request.getPayload());
            break;
        case FILE_SYSTEM:
            if (!(request.getPayload() instanceof FileSystemJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for FILE_SYSTEM job");
            }
            validateFileSystemJobPayload((FileSystemJobPayloadDto) request.getPayload());
            break;
        case DB_TO_KAFKA:
            if (!(request.getPayload() instanceof DbToKafkaJobPayloadDto)) {
                throw new InvalidJobConfigurationException("Invalid payload type for DB_TO_KAFKA job");
            }
            validateDbToKafkaJobPayload((DbToKafkaJobPayloadDto) request.getPayload());
            break;
        default:
            throw new InvalidJobConfigurationException("Unsupported job type: " + request.getType());
    }
}

private void validateHttpJobPayload(HttpJobPayloadDto payload) {
    if (payload.getHttpUrl() == null || payload.getHttpUrl().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("HTTP URL is required for HTTP jobs");
    }
    if (payload.getHttpMethod() == null) {
        throw new InvalidJobConfigurationException("HTTP method is required for HTTP jobs");
    }
}

private void validateScriptJobPayload(ScriptJobPayloadDto payload) {
    if (payload.getScript() == null || payload.getScript().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Script content is required for SCRIPT jobs");
    }
}

private void validateCacheJobPayload(CacheJobPayloadDto payload) {
    if (payload.getOperation() == null) {
        throw new InvalidJobConfigurationException("Cache operation is required for CACHE jobs");
    }
    if (!Arrays.asList("WARM", "INVALIDATE", "SYNC", "STATS").contains(payload.getOperation())) {
        throw new InvalidJobConfigurationException("Invalid cache operation: " + payload.getOperation());
    }
    if (payload.getRegion() == null || payload.getRegion().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Cache region is required for CACHE jobs");
    }
}

private void validateMessageQueueJobPayload(MessageQueueJobPayloadDto payload) {
    if (payload.getOperationType() == null || payload.getOperationType().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Queue operation is required for MESSAGE_QUEUE jobs");
    }
    if (!Arrays.asList("PRODUCE", "CONSUME", "MOVE_DLQ", "PURGE").contains(payload.getOperationType().toUpperCase())) {
        throw new InvalidJobConfigurationException("Invalid queue operation: " + payload.getOperationType());
    }
    if (payload.getQueueName() == null || payload.getQueueName().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Queue name is required for MESSAGE_QUEUE jobs");
    }
    // For PRODUCE operations, message body is required
    if ("PRODUCE".equals(payload.getOperationType().toUpperCase()) && 
        (payload.getMessageBody() == null || payload.getMessageBody().trim().isEmpty())) {
        throw new InvalidJobConfigurationException("Message body is required for PRODUCE operations");
    }
    // Validate queue configuration is not null
    if (payload.getQueueConfig() == null) {
        throw new InvalidJobConfigurationException("Queue configuration must not be null");
    }
    // For MOVE_DLQ operations, target queue is required
    if ("MOVE_DLQ".equals(payload.getOperationType().toUpperCase()) && 
        !payload.getQueueConfig().containsKey("targetQueue")) {
        throw new InvalidJobConfigurationException("Target queue is required in queueConfig for MOVE_DLQ operations");
    }
}

private void validateDatabaseJobPayload(DatabaseJobPayloadDto payload) {
    if (payload.getQuery() == null || payload.getQuery().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Query is required for DATABASE jobs");
    }
    if (payload.getDatabaseUrl() == null || payload.getDatabaseUrl().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Database URL is required for DATABASE jobs");
    }
    if (payload.getParameters() == null) {
        throw new InvalidJobConfigurationException("Query parameters must not be null");
    }
}

private void validateFileSystemJobPayload(FileSystemJobPayloadDto payload) {
    if (payload.getPath() == null || payload.getPath().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("File path is required for FILE_SYSTEM jobs");
    }
    if (payload.getOperation() == null || payload.getOperation().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Operation is required for FILE_SYSTEM jobs");
    }
    if (!Arrays.asList("COPY", "MOVE", "DELETE", "PROCESS", "COMPRESS").contains(payload.getOperation().toUpperCase())) {
        throw new InvalidJobConfigurationException("Invalid file system operation: " + payload.getOperation());
    }
    if ((payload.getOperation().equalsIgnoreCase("COPY") || payload.getOperation().equalsIgnoreCase("MOVE")) 
        && (payload.getTargetPath() == null || payload.getTargetPath().trim().isEmpty())) {
        throw new InvalidJobConfigurationException("Target path is required for " + payload.getOperation() + " operations");
    }
    if (payload.getParameters() == null) {
        throw new InvalidJobConfigurationException("File parameters must not be null");
    }
}

private void validateDbToKafkaJobPayload(DbToKafkaJobPayloadDto payload) {
    if (payload.getDatabaseUrl() == null || payload.getDatabaseUrl().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Database URL is required for DB_TO_KAFKA jobs");
    }
    if (payload.getQuery() == null || payload.getQuery().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("SQL query is required for DB_TO_KAFKA jobs");
    }
    if (payload.getKafkaTopic() == null || payload.getKafkaTopic().trim().isEmpty()) {
        throw new InvalidJobConfigurationException("Kafka topic is required for DB_TO_KAFKA jobs");
    }
    if (payload.getQueryParameters() == null) {
        throw new InvalidJobConfigurationException("Query parameters must not be null");
    }
    if (payload.getBatchSize() != null && payload.getBatchSize() <= 0) {
        throw new InvalidJobConfigurationException("Batch size must be positive");
    }
    if (payload.getMaxRecords() != null && payload.getMaxRecords() <= 0) {
        throw new InvalidJobConfigurationException("Max records must be positive");
    }
    if (payload.getMaxRetries() != null && payload.getMaxRetries() < 0) {
        throw new InvalidJobConfigurationException("Max retries cannot be negative");
    }
    // Validate SQL query doesn't contain dangerous operations
    String upperQuery = payload.getQuery().toUpperCase().trim();
    if (upperQuery.matches(".*(\\bDELETE\\b|\\bUPDATE\\b|\\bINSERT\\b|\\bDROP\\b|\\bALTER\\b|\\bCREATE\\b).*")) {
        throw new InvalidJobConfigurationException("Only SELECT queries are allowed for DB_TO_KAFKA jobs");
    }
}

private void handleJobFailure(Job job, JobRun run, String errorMessage) {
    if (shouldRetry(job.getId(), run.getAttempt())) {
        try {
            // Check if retry is already scheduled for this attempt to prevent duplicates
            int nextAttempt = run.getAttempt() + 1;
            boolean retryAlreadyScheduled = jobRunRepository.existsByJobIdAndAttempt(job.getId(), nextAttempt);
            
            if (retryAlreadyScheduled) {
                log.debug("Retry attempt {} already scheduled for job {}, skipping duplicate", nextAttempt, job.getId());
                return;
            }
            
            // Record retry metrics
            jobMetrics.recordJobRetry();
            
            // Calculate next retry time based on backoff strategy
            Instant nextRetryTime = calculateNextRetryTime(job.getId(), run.getAttempt());
            
            // Schedule the retry
            job.setStatus(JobStatus.SCHEDULED);
            job.setNextRunAt(nextRetryTime);
            jobRepository.saveAndFlush(job);
            
            // Create new run for retry attempt
            JobRun retryRun = JobRun.builder()
                    .id(UlidCreator.getUlid().toString())
                    .job(job)
                    .scheduledTime(nextRetryTime)
                    .attempt(nextAttempt)
                    .build();
            
            jobRunRepository.saveAndFlush(retryRun);
            
            // Schedule with Quartz
            try {
                quartzSchedulerService.rescheduleJob(job, nextRetryTime, nextAttempt);
            } catch (SchedulerException e) {
                log.error("Failed to reschedule job {}: {}", job.getId(), e.getMessage(), e);
                markJobAsFailed(job, run, "Failed to schedule retry: " + e.getMessage());
                return;
            }
            
            log.info("Scheduled retry attempt {} for job {}", retryRun.getAttempt(), job.getId());
            
            // Send notification
            notificationService.notifyJobFailure(job, run, errorMessage);
        } catch (Exception e) {
            log.error("Failed to schedule retry for job {}", job.getId(), e);
            markJobAsFailed(job, run, "Failed to schedule retry: " + e.getMessage());
        }
    } else {
        markJobAsFailed(job, run, errorMessage);
    }
}

private void markJobAsFailed(Job job, JobRun run, String reason) {
    job.setStatus(JobStatus.FAILED);
    jobRepository.saveAndFlush(job);
    eventPublisher.publishEvent(new JobFailureEvent(this, job, run, reason));
    notificationService.notifyMaxRetriesExceeded(job, run);
}

}
