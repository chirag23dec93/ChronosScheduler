package com.chronos.service.impl;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.JobRunLog;
import com.chronos.domain.model.payload.HttpJobPayload;
import com.chronos.domain.model.payload.ScriptJobPayload;
import com.chronos.event.JobLogEvent;
import com.chronos.exception.JobExecutionException;
import com.chronos.repository.JobRunLogRepository;
import com.chronos.service.JobExecutorService;
import com.chronos.service.JobService;
import org.springframework.context.ApplicationContext;
import com.chronos.service.executor.DatabaseJobExecutor;
import com.chronos.service.executor.FileSystemJobExecutor;
import com.chronos.service.executor.CacheJobExecutor;
import com.chronos.service.executor.ReportJobExecutor;
import com.chronos.service.executor.MessageQueueJobExecutor;
import com.chronos.service.executor.DbToKafkaJobExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutorServiceImpl implements JobExecutorService {

    private final JobRunLogRepository jobRunLogRepository;
    private final RestTemplate restTemplate;
    private final DatabaseJobExecutor databaseJobExecutor;
    private final FileSystemJobExecutor fileSystemJobExecutor;
    private final CacheJobExecutor cacheJobExecutor;
    private final ReportJobExecutor reportJobExecutor;
    private final MessageQueueJobExecutor messageQueueJobExecutor;
    private final DbToKafkaJobExecutor dbToKafkaJobExecutor;
    private final ApplicationContext applicationContext;
    
    @Value("${app.job.max-concurrent-executions:50}")
    private int maxConcurrentExecutions;
    
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Map<String, Boolean> runningJobs = new ConcurrentHashMap<>();

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void executeJob(Job job, JobRun run) {
        // Check global concurrent execution limit
        if (runningJobs.size() >= maxConcurrentExecutions) {
            log.warn("Maximum concurrent executions reached ({}), skipping job {}", maxConcurrentExecutions, job.getId());
            return;
        }

        // Try to acquire lock
        if (runningJobs.putIfAbsent(job.getId(), Boolean.TRUE) == null) {
            try {
                // Start job in a new transaction
                try {
                    getJobService().markJobAsRunning(job.getId(), run.getId(), getWorkerId());
                } catch (JobExecutionException e) {
                    // Job is already running, skip execution
                    log.warn("Job {} is already running, skipping execution", job.getId());
                    runningJobs.remove(job.getId());
                    return;
                }

                boolean success = false;
                String error = null;

                // Execute job
                try {
                    switch (job.getType()) {
                        case HTTP -> executeHttpJob(job, run);
                        case SCRIPT -> executeScriptJob(job, run);
                        case DUMMY -> executeDummyJob(job, run);
                        case DATABASE -> databaseJobExecutor.execute(job, run);
                        case FILE_SYSTEM -> fileSystemJobExecutor.execute(job, run);
                        case CACHE -> cacheJobExecutor.execute(job, run);
                        case REPORT -> reportJobExecutor.execute(job, run);
                        case MESSAGE_QUEUE -> messageQueueJobExecutor.execute(job, run);
                        case DB_TO_KAFKA -> dbToKafkaJobExecutor.execute(job, run);
                        default -> throw new JobExecutionException("Unsupported job type: " + job.getType());
                    }
                    success = true;
                } catch (Exception e) {
                    error = e.getMessage();
                    throw e;
                } finally {
                    // Always try to complete the job
                    try {
                        getJobService().markJobAsComplete(job.getId(), run.getId(), success, error);
                    } catch (Exception e) {
                        log.error("Failed to mark job {} as complete: {}", job.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Job {} execution failed: {}", job.getId(), e.getMessage());
                handleFailure(job, run, e);
            } finally {
                try {
                    cleanupResources(run);
                } catch (Exception e) {
                    log.error("Error during resource cleanup for job {}", job.getId(), e);
                } finally {
                    runningJobs.remove(job.getId());
                }
            }
        } else {
            log.warn("Job {} is already running, skipping execution", job.getId());
        }
    }

    @Override
    public void executeHttpJob(Job job, JobRun run) {
        if (!(job.getPayload() instanceof HttpJobPayload httpPayload)) {
            throw new JobExecutionException("Invalid payload type for HTTP job");
        }

        logOutput(run, "INFO", "Starting HTTP request");
        
        HttpHeaders headers = new HttpHeaders();
        if (httpPayload.getHttpHeaders() != null) {
            httpPayload.getHttpHeaders().forEach(headers::add);
        }
        
        HttpEntity<?> entity = new HttpEntity<>(
                httpPayload.getHttpBody(),
                headers
        );
        
        ResponseEntity<String> response = restTemplate.exchange(
                httpPayload.getHttpUrl(),
                HttpMethod.valueOf(httpPayload.getHttpMethod()),
                entity,
                String.class
        );
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new JobExecutionException("HTTP request failed with status: " + response.getStatusCode());
        }
        
        logOutput(run, "INFO", "HTTP job completed successfully", 
                Map.of("statusCode", response.getStatusCode().value()));
    }

    @Override
    public void executeScriptJob(Job job, JobRun run) {
        if (!(job.getPayload() instanceof ScriptJobPayload scriptPayload)) {
            throw new JobExecutionException("Invalid payload type for Script job");
        }

        logOutput(run, "INFO", "Starting script execution");
        
        try {
            Process process = new ProcessBuilder("bash", "-c", scriptPayload.getScript())
                    .redirectErrorStream(true)
                    .start();
            
            runningProcesses.put(run.getId(), process);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    logOutput(run, "INFO", line);
                }
            }
            
            if (!process.waitFor(30, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new JobExecutionException("Script execution timed out");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new JobExecutionException("Script failed with exit code: " + exitCode);
            }
            
            logOutput(run, "INFO", "Script completed successfully", 
                    Map.of("exitCode", exitCode));
            
        } catch (Exception e) {
            throw new JobExecutionException("Script execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void executeDummyJob(Job job, JobRun run) {
        logOutput(run, "INFO", "Starting dummy job execution");
        
        try {
            // Simulate some work
            Thread.sleep(5000);
            logOutput(run, "INFO", "Dummy job completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Dummy job interrupted");
        }
    }

    @Override
    public void logOutput(JobRun run, String level, String message) {
        logOutput(run, level, message, null);
    }

    @Override
    public void logOutput(JobRun run, String level, String message, Object context) {
        JobRunLog runLog = JobRunLog.builder()
                .run(run)
                .timestamp(Instant.now())
                .level(level)
                .message(message)
                .context(context instanceof Map ? (Map<String, Object>) context : null)
                .build();
        
        jobRunLogRepository.save(runLog);
        // Use the SLF4J logger from @Slf4j annotation
        log.info("Job run log saved - [{}] {} - {}", run.getId(), level, message);
    }

    @EventListener
    public void handleJobLogEvent(JobLogEvent event) {
        logOutput(event.getRun(), event.getLevel(), event.getMessage(), event.getContext());
    }

    @Override
    public void handleFailure(Job job, JobRun run, Throwable error) {
        String errorMessage = error.getMessage();
        logOutput(run, "ERROR", "Job execution failed: " + errorMessage, 
                Map.of("stackTrace", error.toString()));
        
        getJobService().markJobAsComplete(job.getId(), run.getId(), false, errorMessage);
    }

    @Override
    public void cleanupResources(JobRun run) {
        Process process = runningProcesses.remove(run.getId());
        if (process != null) {
            process.destroyForcibly();
        }
    }

    @Override
    public boolean isJobRunning(String jobId) {
        return runningJobs.containsKey(jobId);
    }

    @Override
    public void cancelRunningJob(String jobId) {
        if (isJobRunning(jobId)) {
            runningJobs.remove(jobId);
            // The job will be marked as cancelled in its next execution cycle
        }
    }

    @Override
    public int getRunningJobCount() {
        return runningJobs.size();
    }

    private String getWorkerId() {
        return System.getProperty("hostname", "worker-" + 
                System.currentTimeMillis() % 10000);
    }

    private JobService getJobService() {
        return applicationContext.getBean(JobService.class);
    }
}
