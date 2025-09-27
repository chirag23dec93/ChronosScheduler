package com.chronos.service;

import com.chronos.domain.model.Job;
import org.quartz.SchedulerException;

import java.time.Instant;

public interface QuartzSchedulerService {
    
    void scheduleJob(Job job) throws SchedulerException;
    
    void rescheduleJob(Job job, Instant nextRunTime) throws SchedulerException;

    void rescheduleJob(Job job, Instant nextRunTime, int attempt) throws SchedulerException;
    
    void pauseJob(String jobId) throws SchedulerException;
    
    void resumeJob(String jobId) throws SchedulerException;
    
    void deleteJob(String jobId) throws SchedulerException;
    
    void triggerJobNow(String jobId) throws SchedulerException;
    
    boolean isJobScheduled(String jobId) throws SchedulerException;
    
    Instant getNextFireTime(String jobId) throws SchedulerException;
    
    void initializeScheduler() throws SchedulerException;
    
    void shutdownScheduler() throws SchedulerException;

    void stopAllJobs() throws SchedulerException;
    
    String getSchedulerInstanceId() throws SchedulerException;
    
    boolean isSchedulerClustered() throws SchedulerException;
}
