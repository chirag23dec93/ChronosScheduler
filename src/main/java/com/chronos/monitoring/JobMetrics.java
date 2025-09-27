package com.chronos.monitoring;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class JobMetrics {

    private final MeterRegistry registry;

    private Counter jobSubmissionCounter;
    private Counter jobSuccessCounter;
    private Counter jobFailureCounter;
    private Counter jobRetryCounter;
    private AtomicInteger runningJobsGauge;
    private Timer jobExecutionTimer;
    private DistributionSummary jobPayloadSize;

    @PostConstruct
    public void init() {
        // Job submission metrics
        jobSubmissionCounter = Counter.builder("chronos.jobs.submissions")
                .description("Total number of jobs submitted")
                .register(registry);

        // Job execution outcome metrics
        jobSuccessCounter = Counter.builder("chronos.jobs.executions")
                .tag("outcome", "success")
                .description("Number of successful job executions")
                .register(registry);

        jobFailureCounter = Counter.builder("chronos.jobs.executions")
                .tag("outcome", "failure")
                .description("Number of failed job executions")
                .register(registry);

        // Job retry metrics
        jobRetryCounter = Counter.builder("chronos.jobs.retries")
                .description("Number of job retries")
                .register(registry);

        // Running jobs gauge
        runningJobsGauge = registry.gauge("chronos.jobs.running",
                Tags.empty(),
                new AtomicInteger(0));

        // Job execution timing
        jobExecutionTimer = Timer.builder("chronos.jobs.duration")
                .description("Job execution duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Job payload size
        jobPayloadSize = DistributionSummary.builder("chronos.jobs.payload.size")
                .description("Job payload size in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordJobSubmission() {
        jobSubmissionCounter.increment();
    }

    public void recordJobSuccess() {
        jobSuccessCounter.increment();
    }

    public void recordJobFailure() {
        jobFailureCounter.increment();
    }

    public void recordJobRetry() {
        jobRetryCounter.increment();
    }

    public void setRunningJobs(int count) {
        runningJobsGauge.set(count);
    }

    public Timer.Sample startJobExecution() {
        return Timer.start(registry);
    }

    public void stopJobExecution(Timer.Sample sample) {
        sample.stop(jobExecutionTimer);
    }

    public void recordPayloadSize(long bytes) {
        jobPayloadSize.record(bytes);
    }
}
