package com.chronos.service.impl;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.DLQEvent;
import com.chronos.repository.DLQEventRepository;
import com.chronos.repository.JobRepository;
import com.chronos.domain.model.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobFailureHandler {

    private final DLQEventRepository dlqEventRepository;
    private final JobRepository jobRepository;

    @EventListener
    @Transactional
    public void handleJobFailure(JobFailureEvent event) {
        Job job = event.getJob();
        JobRun run = event.getRun();
        String reason = event.getReason();

        // Only add to DLQ if we've exceeded max retries
        if (run.getAttempt() >= job.getRetryPolicy().getMaxAttempts()) {
            DLQEvent dlqEvent = DLQEvent.builder()
                    .job(job)
                    .lastRun(run)
                    .reason(reason)
                    .createdAt(Instant.now())
                    .build();

            dlqEventRepository.save(dlqEvent);
            
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            
            log.info("Added job {} to DLQ after {} attempts: {}", 
                    job.getId(), run.getAttempt(), reason);
        } else {
            log.info("Job {} failed but will retry. Attempt {}/{}: {}", 
                    job.getId(), run.getAttempt(), 
                    job.getRetryPolicy().getMaxAttempts(), reason);
        }
    }
}
