package com.chronos.service;

import com.chronos.api.dto.job.CreateJobRequest;
import com.chronos.api.dto.job.JobResponse;
import com.chronos.api.dto.job.JobRunLogDto;
import com.chronos.api.dto.job.JobRunSummaryDto;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface JobService {
    
    JobResponse createJob(CreateJobRequest request);
    
    JobResponse getJob(String jobId);
    
    Page<JobResponse> getJobs(JobStatus status, String nameContains, Instant nextRunFrom,
                             Instant nextRunTo, Pageable pageable);
    
    JobResponse updateJob(String jobId, CreateJobRequest request);
    
    void deleteJob(String jobId);
    
    JobResponse scheduleJob(String jobId, Instant runAt);
    
    JobResponse cancelJob(String jobId);

    void updateAllJobStatus(JobStatus status);

    JobResponse pauseJob(String jobId);
    
    JobResponse resumeJob(String jobId);
    
    JobResponse triggerJobNow(String jobId);
    
    Page<JobRunSummaryDto> getJobRuns(String jobId, Pageable pageable);
    
    JobRunSummaryDto getJobRun(String jobId, String runId);
    
    Page<JobRunLogDto> getJobRunLogs(String jobId, String runId, Pageable pageable);
    
    List<JobResponse> findReadyJobs(int limit);
    
    void markJobAsRunning(String jobId, String runId, String workerId);
    
    void markJobAsComplete(String jobId, String runId, boolean success, String errorMessage);
    
    boolean shouldRetry(String jobId, int currentAttempt);
    
    Instant calculateNextRetryTime(String jobId, int currentAttempt);

    Job findJobOrThrow(String jobId);
}
