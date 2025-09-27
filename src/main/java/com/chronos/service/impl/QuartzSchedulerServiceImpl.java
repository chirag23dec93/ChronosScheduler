package com.chronos.service.impl;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.JobSchedule;
import com.chronos.domain.model.enums.JobStatus;
import com.chronos.domain.model.enums.ScheduleType;
import com.chronos.service.JobExecutorService;
import com.chronos.service.JobService;
import com.chronos.service.QuartzSchedulerService;
import com.chronos.repository.JobRunRepository;
import com.chronos.util.SpringContext;
import org.springframework.context.ApplicationContext;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.Set;
import org.quartz.CronScheduleBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuartzSchedulerServiceImpl implements QuartzSchedulerService {

    private final Scheduler scheduler;
    private final ApplicationContext applicationContext;

    @PostConstruct
    @Override
    public void initializeScheduler() throws SchedulerException {
        scheduler.start();
        log.info("Quartz Scheduler started with instance ID: {}", scheduler.getSchedulerInstanceId());
    }

    @Override
    public void stopAllJobs() throws SchedulerException {
        log.info("Stopping all jobs...");
        
        // Clear all triggers first
        scheduler.clear();
        
        // Get all job keys
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        
        // Delete each job
        for (JobKey jobKey : jobKeys) {
            scheduler.deleteJob(jobKey);
            log.info("Deleted job {}", jobKey);
        }
        
        // Update all job statuses to CANCELLED
        getJobService().updateAllJobStatus(JobStatus.CANCELLED);
        
        log.info("All jobs stopped and cancelled");
    }

    @PreDestroy
    @Override
    public void shutdownScheduler() throws SchedulerException {
        scheduler.shutdown(true);
        log.info("Quartz Scheduler shut down");
    }

    @Override
    public void scheduleJob(Job job) throws SchedulerException {
        // First, delete any existing job/trigger
        JobKey jobKey = JobKey.jobKey(job.getId());
        
        // Delete any existing job and all its triggers
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        
        // For interval jobs, we use a unique trigger key with timestamp
        String triggerKeyName = job.getSchedule().getScheduleType() == ScheduleType.INTERVAL ?
                String.format("%s-%d", job.getId(), System.currentTimeMillis()) :
                job.getId();
        TriggerKey triggerKey = TriggerKey.triggerKey(triggerKeyName);
        
        // Create new job detail
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", job.getId());
        jobDataMap.put("attempt", 1);
        
        JobDetail jobDetail = JobBuilder.newJob(QuartzJobExecutor.class)
                .withIdentity(jobKey)
                .usingJobData(jobDataMap)
                .storeDurably()
                .requestRecovery(false)
                .build();
        
        // Create trigger
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withPriority(1);
        
        // Always start now for initial trigger
        builder.startNow();
        
        // Add schedule
        JobSchedule schedule = job.getSchedule();
        switch (schedule.getScheduleType()) {
            case ONCE -> {
                builder.startAt(Date.from(schedule.getRunAt()));
                builder.withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow());
            }
            case CRON -> {
                builder.withSchedule(CronScheduleBuilder
                        .cronSchedule(schedule.getCronExpression())
                        .inTimeZone(TimeZone.getTimeZone(schedule.getTimezone()))
                        .withMisfireHandlingInstructionFireAndProceed());
            }
            case INTERVAL -> {
                builder.withSchedule(SimpleScheduleBuilder
                        .simpleSchedule()
                        .withIntervalInSeconds(schedule.getIntervalSeconds().intValue())
                        .repeatForever()
                        .withMisfireHandlingInstructionFireNow());
            }
        }
        
        // Schedule the job
        scheduler.addJob(jobDetail, true);
        scheduler.scheduleJob(builder.build());
        
        log.info("Scheduled job {} with trigger {} and next fire time {}", 
                job.getId(), triggerKey, scheduler.getTrigger(triggerKey).getNextFireTime());
    }

    @Override
    public void rescheduleJob(Job job, Instant nextRunTime) throws SchedulerException {
        rescheduleJob(job, nextRunTime, 1);
    }

    @Override
    public synchronized void rescheduleJob(Job job, Instant nextRunTime, int attempt) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(job.getId());
        JobKey jobKey = JobKey.jobKey(job.getId());
        
        // Delete any existing job/trigger
        if (scheduler.checkExists(triggerKey)) {
            scheduler.unscheduleJob(triggerKey);
        }
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
        
        // Create new job data map with updated attempt count
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobId", job.getId());
        jobDataMap.put("attempt", attempt);
        
        // Create new job detail
        JobDetail jobDetail = JobBuilder.newJob(QuartzJobExecutor.class)
                .withIdentity(jobKey)
                .usingJobData(jobDataMap)
                .storeDurably()
                .requestRecovery(false)
                .build();
        
        // Create new trigger with updated fire time
        Trigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobKey)
                .startAt(Date.from(nextRunTime))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                    .withMisfireHandlingInstructionFireNow())
                .withPriority(10)  // Higher priority for retry triggers
                .build();
        
        // Schedule both job and trigger
        scheduler.scheduleJob(jobDetail, newTrigger);
        
        log.info("Rescheduled job {} (attempt {}) to run at {}", job.getId(), attempt, nextRunTime);
    }

    @Override
    public void pauseJob(String jobId) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobId));
        log.info("Paused job {}", jobId);
    }

    @Override
    public void resumeJob(String jobId) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobId));
        log.info("Resumed job {}", jobId);
    }

    @Override
    public void deleteJob(String jobId) throws SchedulerException {
        // Get all triggers for this job
        JobKey jobKey = JobKey.jobKey(jobId);
        if (scheduler.checkExists(jobKey)) {
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            
            // Delete all triggers first
            for (Trigger trigger : triggers) {
                TriggerKey triggerKey = trigger.getKey();
                if (scheduler.checkExists(triggerKey)) {
                    scheduler.unscheduleJob(triggerKey);
                    log.info("Unscheduled trigger {} for job {}", triggerKey, jobId);
                }
            }
            
            // Then delete the job
            scheduler.deleteJob(jobKey);
            log.info("Deleted job {}", jobId);
        } else {
            log.warn("Job {} does not exist in Quartz", jobId);
        }
    }

    @Override
    public void triggerJobNow(String jobId) throws SchedulerException {
        scheduler.triggerJob(JobKey.jobKey(jobId));
        log.info("Triggered job {} immediately", jobId);
    }

    @Override
    public boolean isJobScheduled(String jobId) throws SchedulerException {
        return scheduler.checkExists(JobKey.jobKey(jobId));
    }

    @Override
    public Instant getNextFireTime(String jobId) throws SchedulerException {
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(jobId));
        return trigger != null && trigger.getNextFireTime() != null ? 
                trigger.getNextFireTime().toInstant() : null;
    }

    @Override
    public String getSchedulerInstanceId() throws SchedulerException {
        return scheduler.getSchedulerInstanceId();
    }

    @Override
    public boolean isSchedulerClustered() throws SchedulerException {
        return scheduler.getMetaData().isJobStoreClustered();
    }

    private JobService getJobService() {
        return applicationContext.getBean(JobService.class);
    }

    public static class QuartzJobExecutor implements org.quartz.Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            String jobId = context.getJobDetail().getJobDataMap().getString("jobId");
            log.info("Quartz triggered execution of job {}", jobId);
            
            JobExecutorService jobExecutorService = null;
            JobService jobService = null;
            Job job = null;
            JobRun jobRun = null;
            
            try {
                // Get Spring beans
                jobExecutorService = SpringContext.getBean(JobExecutorService.class);
                jobService = SpringContext.getBean(JobService.class);
                
                // Get job
                job = jobService.findJobOrThrow(jobId);
                
                // Check if job is cancelled or in terminal state
                if (job.getStatus() == JobStatus.CANCELLED) {
                    log.info("Job {} is cancelled, removing from Quartz", jobId);
                    context.getScheduler().deleteJob(context.getJobDetail().getKey());
                    return;
                }
                
                // Check if job is in terminal state
                // For recurring jobs (CRON/INTERVAL), only FAILED is terminal
                // For one-time jobs (ONCE), both FAILED and SUCCEEDED are terminal
                boolean isTerminal = false;
                if (job.getStatus() == JobStatus.FAILED) {
                    isTerminal = true;
                } else if (job.getStatus() == JobStatus.SUCCEEDED && 
                           job.getSchedule().getScheduleType() == ScheduleType.ONCE) {
                    isTerminal = true;
                }
                
                if (isTerminal) {
                    log.warn("Job {} is in terminal state {}, skipping execution", 
                            jobId, job.getStatus());
                    return;
                }
                
                // Check if job is already running
                if (job.getStatus() == JobStatus.RUNNING) {
                    log.warn("Job {} is already running, skipping execution", jobId);
                    return;
                }
                
                // Get attempt count from job data map
                int attempt = context.getMergedJobDataMap().getInt("attempt");
                log.info("Starting job {} execution attempt {}", jobId, attempt);

                // Try to find existing JobRun for this attempt, or create new one
                JobRunRepository jobRunRepository = SpringContext.getBean(JobRunRepository.class);
                Optional<JobRun> existingRun = jobRunRepository.findByJobAndAttempt(job, attempt);
                
                if (existingRun.isPresent()) {
                    // Reuse existing JobRun and update it for execution
                    jobRun = existingRun.get();
                    jobRun.setStartTime(Instant.now());
                    jobRun.setWorkerId(context.getScheduler().getSchedulerInstanceId());
                    log.debug("Reusing existing JobRun {} for job {} attempt {}", jobRun.getId(), jobId, attempt);
                } else {
                    // Create a new JobRun (for initial execution)
                    jobRun = JobRun.builder()
                            .id(UlidCreator.getUlid().toString())
                            .job(job)
                            .scheduledTime(Instant.now())
                            .startTime(Instant.now())
                            .workerId(context.getScheduler().getSchedulerInstanceId())
                            .attempt(attempt)
                            .build();
                    log.debug("Created new JobRun {} for job {} attempt {}", jobRun.getId(), jobId, attempt);
                }
                
                // Execute the job
                jobExecutorService.executeJob(job, jobRun);
            } catch (Exception e) {
                String error = String.format("Failed to execute job %s: %s", jobId, e.getMessage());
                log.error(error, e);
                
                // Try to mark job as failed if we have enough context
                if (job != null && jobRun != null && jobService != null) {
                    try {
                        jobService.markJobAsComplete(job.getId(), jobRun.getId(), false, error);
                    } catch (Exception ex) {
                        log.error("Failed to mark job {} as failed: {}", jobId, ex.getMessage());
                        // Force job status update
                        try {
                            job.setStatus(JobStatus.FAILED);
                            jobService.findJobOrThrow(jobId).setStatus(JobStatus.FAILED);
                        } catch (Exception updateEx) {
                            log.error("Failed to force update job {} status: {}", jobId, updateEx.getMessage());
                        }
                    }
                }
                
                throw new JobExecutionException(e, false);
            }
        }
    }
}
