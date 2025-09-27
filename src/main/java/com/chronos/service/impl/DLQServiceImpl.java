package com.chronos.service.impl;

import com.chronos.api.dto.dlq.DLQEventResponse;
import com.chronos.api.mapper.DLQEventMapper;
import com.chronos.domain.model.DLQEvent;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.exception.ResourceNotFoundException;
import com.chronos.repository.DLQEventRepository;
import com.chronos.service.DLQService;
import com.chronos.service.JobService;
import com.chronos.service.QuartzSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DLQServiceImpl implements DLQService {

    private final DLQEventRepository dlqEventRepository;
    private final DLQEventMapper dlqEventMapper;
    private final JobService jobService;
    private final QuartzSchedulerService schedulerService;

    @Override
    @Transactional
    public void addToDLQ(Job job, JobRun lastRun, String reason) {
        DLQEvent event = DLQEvent.builder()
                .job(job)
                .lastRun(lastRun)
                .reason(reason)
                .createdAt(Instant.now())
                .build();
        
        dlqEventRepository.save(event);
        log.info("Added job {} to DLQ: {}", job.getId(), reason);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DLQEventResponse> getDLQEvents(Pageable pageable) {
        return dlqEventRepository.findAll(pageable)
                .map(dlqEventMapper::toDLQEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DLQEventResponse> searchDLQEvents(String reasonContains, Pageable pageable) {
        return dlqEventRepository.searchByReason(reasonContains, pageable)
                .map(dlqEventMapper::toDLQEventResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DLQEventResponse getDLQEvent(Long eventId) {
        DLQEvent event = dlqEventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("DLQEvent", eventId.toString()));
        return dlqEventMapper.toDLQEventResponse(event);
    }

    @Override
    @Transactional
    public void replayDLQEvent(Long eventId) {
        DLQEvent event = dlqEventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("DLQEvent", eventId.toString()));
        
        try {
            // Reset job status and schedule it for immediate execution
            schedulerService.triggerJobNow(event.getJob().getId());
            log.info("Replayed DLQ event {} for job {}", eventId, event.getJob().getId());
        } catch (SchedulerException e) {
            log.error("Failed to replay DLQ event {} for job {}", eventId, event.getJob().getId(), e);
            throw new RuntimeException("Failed to replay DLQ event: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void replayAllDLQEvents() {
        dlqEventRepository.findUnresolvedEvents().forEach(event -> {
            try {
                replayDLQEvent(event.getId());
            } catch (Exception e) {
                log.error("Failed to replay DLQ event {} for job {}", 
                        event.getId(), event.getJob().getId(), e);
            }
        });
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Run hourly
    @Transactional
    public void cleanupResolvedEvents() {
        try {
            // Find all DLQ events older than 7 days that have been resolved
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            dlqEventRepository.findAll().forEach(event -> {
                if (event.getCreatedAt().isBefore(cutoff) && 
                    event.getJob().getStatus() != com.chronos.domain.model.enums.JobStatus.FAILED) {
                    dlqEventRepository.delete(event);
                    log.info("Cleaned up resolved DLQ event {} for job {}", 
                            event.getId(), event.getJob().getId());
                }
            });
        } catch (Exception e) {
            log.error("Failed to cleanup resolved DLQ events", e);
        }
    }
}
