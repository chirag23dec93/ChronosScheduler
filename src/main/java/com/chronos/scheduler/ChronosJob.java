package com.chronos.scheduler;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.service.JobExecutorService;
import com.chronos.service.JobService;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChronosJob extends QuartzJobBean {

    private final JobService jobService;
    private final JobExecutorService jobExecutorService;

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String jobId = dataMap.getString("jobId");
        String runId = UlidCreator.getUlid().toString();

        try {
            Job job = jobService.findJobOrThrow(jobId);
            JobRun run = JobRun.builder()
                    .id(runId)
                    .job(job)
                    .scheduledTime(context.getScheduledFireTime().toInstant())
                    .build();

            jobExecutorService.executeJob(job, run);
        } catch (Exception e) {
            log.error("Failed to execute job {}: {}", jobId, e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
