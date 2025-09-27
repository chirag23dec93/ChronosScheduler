package com.chronos.monitoring;

import com.chronos.domain.model.enums.JobStatus;
import com.chronos.repository.JobRepository;
import com.chronos.service.QuartzSchedulerService;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobSchedulerHealthIndicator implements HealthIndicator {

    private final QuartzSchedulerService schedulerService;
    private final JobRepository jobRepository;

    @Override
    public Health health() {
        try {
            String instanceId = schedulerService.getSchedulerInstanceId();
            boolean isClustered = schedulerService.isSchedulerClustered();
            long runningJobs = jobRepository.countByStatus(JobStatus.RUNNING);

            return Health.up()
                    .withDetail("instanceId", instanceId)
                    .withDetail("clustered", isClustered)
                    .withDetail("runningJobs", runningJobs)
                    .build();

        } catch (SchedulerException e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
