package com.chronos.service;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;

public interface JobExecutorService {
    
    void executeJob(Job job, JobRun run);
    
    void executeHttpJob(Job job, JobRun run);
    
    void executeScriptJob(Job job, JobRun run);
    
    void executeDummyJob(Job job, JobRun run);
    
    void logOutput(JobRun run, String level, String message);
    
    void logOutput(JobRun run, String level, String message, Object context);
    
    void handleFailure(Job job, JobRun run, Throwable error);
    
    void cleanupResources(JobRun run);
    
    boolean isJobRunning(String jobId);
    
    void cancelRunningJob(String jobId);
    
    int getRunningJobCount();
}
