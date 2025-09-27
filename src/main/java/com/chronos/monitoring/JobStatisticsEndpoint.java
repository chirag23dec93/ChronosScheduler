package com.chronos.monitoring;

import com.chronos.domain.model.enums.JobStatus;
import com.chronos.repository.JobRepository;
import com.chronos.repository.JobRunRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "jobstats")
@RequiredArgsConstructor
public class JobStatisticsEndpoint {

    private final JobRepository jobRepository;
    private final JobRunRepository jobRunRepository;

    @ReadOperation
    public JobStatistics getStatistics() {
        JobStatistics stats = new JobStatistics();
        
        // Job counts by status
        for (JobStatus status : JobStatus.values()) {
            stats.getJobsByStatus().put(status.name(), jobRepository.countByStatus(status));
        }

        // Recent execution statistics
        Instant now = Instant.now();
        Instant lastHour = now.minus(1, ChronoUnit.HOURS);
        Instant lastDay = now.minus(1, ChronoUnit.DAYS);

        stats.setLastHourSuccessCount(jobRunRepository.countByJobAndOutcome(null, 
                com.chronos.domain.model.enums.JobOutcome.SUCCESS));
        stats.setLastHourFailureCount(jobRunRepository.countByJobAndOutcome(null, 
                com.chronos.domain.model.enums.JobOutcome.FAILURE));

        // Average durations
        Double lastHourAvgDuration = jobRunRepository.getAverageDuration(null, 
                com.chronos.domain.model.enums.JobOutcome.SUCCESS, lastHour);
        Double lastDayAvgDuration = jobRunRepository.getAverageDuration(null, 
                com.chronos.domain.model.enums.JobOutcome.SUCCESS, lastDay);

        stats.setLastHourAverageDuration(lastHourAvgDuration != null ? lastHourAvgDuration : 0.0);
        stats.setLastDayAverageDuration(lastDayAvgDuration != null ? lastDayAvgDuration : 0.0);

        return stats;
    }

    @Data
    public static class JobStatistics {
        private Map<String, Long> jobsByStatus = new HashMap<>();
        private long lastHourSuccessCount;
        private long lastHourFailureCount;
        private double lastHourAverageDuration;
        private double lastDayAverageDuration;
    }
}
