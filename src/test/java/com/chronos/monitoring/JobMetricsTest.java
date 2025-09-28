package com.chronos.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JobMetricsTest {

    private MeterRegistry meterRegistry;
    private JobMetrics jobMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        jobMetrics = new JobMetrics(meterRegistry);
    }

    @Test
    void recordJobSubmission_IncrementsCounter() {
        // Given
        double initialCount = getCounterValue("chronos_jobs_submissions_total");

        // When
        jobMetrics.recordJobSubmission();

        // Then
        double finalCount = getCounterValue("chronos_jobs_submissions_total");
        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void recordJobSuccess_IncrementsSuccessCounter() {
        // Given
        double initialCount = getCounterValue("chronos_jobs_executions_total", "outcome", "success");

        // When
        jobMetrics.recordJobSuccess();

        // Then
        double finalCount = getCounterValue("chronos_jobs_executions_total", "outcome", "success");
        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void recordJobFailure_IncrementsFailureCounter() {
        // Given
        double initialCount = getCounterValue("chronos_jobs_executions_total", "outcome", "failure");

        // When
        jobMetrics.recordJobFailure();

        // Then
        double finalCount = getCounterValue("chronos_jobs_executions_total", "outcome", "failure");
        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void recordJobRetry_IncrementsRetryCounter() {
        // Given
        double initialCount = getCounterValue("chronos_jobs_retries_total");

        // When
        jobMetrics.recordJobRetry();

        // Then
        double finalCount = getCounterValue("chronos_jobs_retries_total");
        assertEquals(initialCount + 1, finalCount);
    }

    @Test
    void recordJobExecution_RecordsExecutionTime() {
        // Given
        Duration executionTime = Duration.ofSeconds(5);

        // When
        jobMetrics.recordJobExecution(executionTime);

        // Then
        Timer timer = meterRegistry.find("chronos_jobs_execution_duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.SECONDS) >= 5.0);
    }

    @Test
    void incrementRunningJobs_UpdatesGauge() {
        // Given
        double initialValue = getGaugeValue("chronos_jobs_running");

        // When
        jobMetrics.incrementRunningJobs();

        // Then
        double finalValue = getGaugeValue("chronos_jobs_running");
        assertEquals(initialValue + 1, finalValue);
    }

    @Test
    void decrementRunningJobs_UpdatesGauge() {
        // Given
        jobMetrics.incrementRunningJobs(); // Start with 1
        double initialValue = getGaugeValue("chronos_jobs_running");

        // When
        jobMetrics.decrementRunningJobs();

        // Then
        double finalValue = getGaugeValue("chronos_jobs_running");
        assertEquals(initialValue - 1, finalValue);
    }

    @Test
    void decrementRunningJobs_DoesNotGoBelowZero() {
        // Given - Start with 0 running jobs

        // When
        jobMetrics.decrementRunningJobs();

        // Then
        double finalValue = getGaugeValue("chronos_jobs_running");
        assertEquals(0.0, finalValue);
    }

    @Test
    void multipleJobSubmissions_CountsCorrectly() {
        // Given
        int submissions = 10;

        // When
        for (int i = 0; i < submissions; i++) {
            jobMetrics.recordJobSubmission();
        }

        // Then
        double finalCount = getCounterValue("chronos_jobs_submissions_total");
        assertEquals(submissions, finalCount);
    }

    @Test
    void mixedJobOutcomes_CountsCorrectly() {
        // Given
        int successes = 7;
        int failures = 3;

        // When
        for (int i = 0; i < successes; i++) {
            jobMetrics.recordJobSuccess();
        }
        for (int i = 0; i < failures; i++) {
            jobMetrics.recordJobFailure();
        }

        // Then
        double successCount = getCounterValue("chronos_jobs_executions_total", "outcome", "success");
        double failureCount = getCounterValue("chronos_jobs_executions_total", "outcome", "failure");
        
        assertEquals(successes, successCount);
        assertEquals(failures, failureCount);
    }

    @Test
    void multipleRetries_CountsCorrectly() {
        // Given
        int retries = 5;

        // When
        for (int i = 0; i < retries; i++) {
            jobMetrics.recordJobRetry();
        }

        // Then
        double retryCount = getCounterValue("chronos_jobs_retries_total");
        assertEquals(retries, retryCount);
    }

    @Test
    void runningJobsTracking_WorksCorrectly() {
        // Given
        assertEquals(0.0, getGaugeValue("chronos_jobs_running"));

        // When - Simulate jobs starting and finishing
        jobMetrics.incrementRunningJobs(); // Job 1 starts
        assertEquals(1.0, getGaugeValue("chronos_jobs_running"));

        jobMetrics.incrementRunningJobs(); // Job 2 starts
        assertEquals(2.0, getGaugeValue("chronos_jobs_running"));

        jobMetrics.incrementRunningJobs(); // Job 3 starts
        assertEquals(3.0, getGaugeValue("chronos_jobs_running"));

        jobMetrics.decrementRunningJobs(); // Job 1 finishes
        assertEquals(2.0, getGaugeValue("chronos_jobs_running"));

        jobMetrics.decrementRunningJobs(); // Job 2 finishes
        assertEquals(1.0, getGaugeValue("chronos_jobs_running"));

        jobMetrics.decrementRunningJobs(); // Job 3 finishes
        assertEquals(0.0, getGaugeValue("chronos_jobs_running"));
    }

    @Test
    void executionTimeRecording_HandlesMultipleExecutions() {
        // Given
        Duration[] executionTimes = {
            Duration.ofMillis(100),
            Duration.ofMillis(500),
            Duration.ofSeconds(2),
            Duration.ofSeconds(10)
        };

        // When
        for (Duration duration : executionTimes) {
            jobMetrics.recordJobExecution(duration);
        }

        // Then
        Timer timer = meterRegistry.find("chronos_jobs_execution_duration").timer();
        assertNotNull(timer);
        assertEquals(executionTimes.length, timer.count());
        
        // Total time should be sum of all executions (approximately)
        double expectedTotalSeconds = 0.1 + 0.5 + 2.0 + 10.0; // 12.6 seconds
        double actualTotalSeconds = timer.totalTime(java.util.concurrent.TimeUnit.SECONDS);
        assertEquals(expectedTotalSeconds, actualTotalSeconds, 0.1);
    }

    @Test
    void executionTimeRecording_HandlesZeroDuration() {
        // Given
        Duration zeroDuration = Duration.ZERO;

        // When
        jobMetrics.recordJobExecution(zeroDuration);

        // Then
        Timer timer = meterRegistry.find("chronos_jobs_execution_duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertEquals(0.0, timer.totalTime(java.util.concurrent.TimeUnit.SECONDS), 0.001);
    }

    @Test
    void allMetrics_InitializedCorrectly() {
        // When - Access all metrics to ensure they're initialized
        jobMetrics.recordJobSubmission();
        jobMetrics.recordJobSuccess();
        jobMetrics.recordJobFailure();
        jobMetrics.recordJobRetry();
        jobMetrics.recordJobExecution(Duration.ofSeconds(1));
        jobMetrics.incrementRunningJobs();

        // Then - All metrics should exist
        assertNotNull(meterRegistry.find("chronos_jobs_submissions_total").counter());
        assertNotNull(meterRegistry.find("chronos_jobs_executions_total").tag("outcome", "success").counter());
        assertNotNull(meterRegistry.find("chronos_jobs_executions_total").tag("outcome", "failure").counter());
        assertNotNull(meterRegistry.find("chronos_jobs_retries_total").counter());
        assertNotNull(meterRegistry.find("chronos_jobs_execution_duration").timer());
        assertNotNull(meterRegistry.find("chronos_jobs_running").gauge());
    }

    @Test
    void concurrentAccess_ThreadSafe() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        // When - Multiple threads accessing metrics concurrently
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    jobMetrics.recordJobSubmission();
                    jobMetrics.recordJobSuccess();
                    jobMetrics.incrementRunningJobs();
                    jobMetrics.decrementRunningJobs();
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - Counts should be consistent
        double submissions = getCounterValue("chronos_jobs_submissions_total");
        double successes = getCounterValue("chronos_jobs_executions_total", "outcome", "success");
        double runningJobs = getGaugeValue("chronos_jobs_running");

        assertEquals(threadCount * operationsPerThread, submissions);
        assertEquals(threadCount * operationsPerThread, successes);
        assertEquals(0.0, runningJobs); // Should be back to 0 after all increments/decrements
    }

    private double getCounterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getCounterValue(String name, String tagKey, String tagValue) {
        Counter counter = meterRegistry.find(name).tag(tagKey, tagValue).counter();
        return counter != null ? counter.count() : 0.0;
    }

    private double getGaugeValue(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge != null ? gauge.value() : 0.0;
    }
}
