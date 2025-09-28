package com.chronos.api.mapper;

import com.chronos.api.dto.job.CreateJobRequest;
import com.chronos.api.dto.job.JobResponse;
import com.chronos.api.dto.job.JobScheduleDto;
import com.chronos.api.dto.job.RetryPolicyDto;
import com.chronos.api.dto.job.payload.HttpJobPayloadDto;
import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.domain.model.payload.HttpJobPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JobMapperTest {

    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper = Mappers.getMapper(JobMapper.class);
    }

    @Test
    void toEntity_CreateJobRequest_Success() {
        // Given
        CreateJobRequest request = CreateJobRequest.builder()
            .name("Test HTTP Job")
            .type(JobType.HTTP)
            .priority(JobPriority.MEDIUM)
            .schedule(JobScheduleDto.builder()
                .scheduleType(ScheduleType.ONCE)
                .runAt(ZonedDateTime.now().plusHours(1))
                .misfirePolicy(MisfirePolicy.FIRE_NOW)
                .build())
            .payload(HttpJobPayloadDto.builder()
                .httpUrl("https://httpbin.org/get")
                .httpMethod("GET")
                .httpHeaders(Map.of("Content-Type", "application/json"))
                .build())
            .retryPolicy(RetryPolicyDto.builder()
                .maxAttempts(3)
                .backoffStrategy(BackoffStrategy.EXPONENTIAL)
                .backoffSeconds(5)
                .retryOn(List.of("5XX", "TIMEOUT"))
                .build())
            .build();

        // When
        Job job = jobMapper.toEntity(request);

        // Then
        assertNotNull(job);
        assertEquals("Test HTTP Job", job.getName());
        assertEquals(JobType.HTTP, job.getType());
        assertEquals(JobPriority.MEDIUM, job.getPriority());
        assertEquals(JobStatus.PENDING, job.getStatus());
        
        // Check schedule
        assertNotNull(job.getSchedule());
        assertEquals(ScheduleType.ONCE, job.getSchedule().getScheduleType());
        assertEquals(MisfirePolicy.FIRE_NOW, job.getSchedule().getMisfirePolicy());
        
        // Check payload
        assertNotNull(job.getPayload());
        assertTrue(job.getPayload() instanceof HttpJobPayload);
        HttpJobPayload httpPayload = (HttpJobPayload) job.getPayload();
        assertEquals("https://httpbin.org/get", httpPayload.getHttpUrl());
        assertEquals("GET", httpPayload.getHttpMethod());
        assertEquals("application/json", httpPayload.getHttpHeaders().get("Content-Type"));
        
        // Check retry policy
        assertNotNull(job.getRetryPolicy());
        assertEquals(3, job.getRetryPolicy().getMaxAttempts());
        assertEquals(BackoffStrategy.EXPONENTIAL, job.getRetryPolicy().getBackoffStrategy());
        assertEquals(5, job.getRetryPolicy().getBackoffSeconds());
        assertTrue(job.getRetryPolicy().getRetryOn().contains("5XX"));
        assertTrue(job.getRetryPolicy().getRetryOn().contains("TIMEOUT"));
    }

    @Test
    void toResponse_Job_Success() {
        // Given
        User owner = new User();
        owner.setEmail("test@example.com");

        HttpJobPayload httpPayload = new HttpJobPayload();
        httpPayload.setHttpUrl("https://httpbin.org/post");
        httpPayload.setHttpMethod("POST");
        httpPayload.setHttpHeaders(Map.of("Authorization", "Bearer token"));
        httpPayload.setHttpBody("{\"test\": true}");

        JobSchedule schedule = new JobSchedule();
        schedule.setScheduleType(ScheduleType.CRON);
        schedule.setCronExpression("0 0 12 * * ?");
        schedule.setTimezone(ZoneId.of("UTC"));
        schedule.setMisfirePolicy(MisfirePolicy.IGNORE);

        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setMaxAttempts(2);
        retryPolicy.setBackoffStrategy(BackoffStrategy.FIXED);
        retryPolicy.setBackoffSeconds(10);
        retryPolicy.setRetryOn(List.of("CONNECTION_ERROR"));

        Job job = new Job();
        job.setId("job-123");
        job.setName("Test CRON Job");
        job.setType(JobType.HTTP);
        job.setStatus(JobStatus.SCHEDULED);
        job.setPriority(JobPriority.HIGH);
        job.setOwner(owner);
        job.setPayload(httpPayload);
        job.setSchedule(schedule);
        job.setRetryPolicy(retryPolicy);
        job.setCreatedAt(Instant.now());

        // When
        JobResponse response = jobMapper.toResponse(job);

        // Then
        assertNotNull(response);
        assertEquals("job-123", response.getId());
        assertEquals("Test CRON Job", response.getName());
        assertEquals(JobType.HTTP, response.getType());
        assertEquals(JobStatus.SCHEDULED, response.getStatus());
        assertEquals(JobPriority.HIGH, response.getPriority());
        assertEquals("test@example.com", response.getOwnerEmail());
        assertNotNull(response.getCreatedAt());

        // Check schedule
        assertNotNull(response.getSchedule());
        assertEquals(ScheduleType.CRON, response.getSchedule().getScheduleType());
        assertEquals("0 0 12 * * ?", response.getSchedule().getCronExpression());
        assertEquals("UTC", response.getSchedule().getTimezone());
        assertEquals(MisfirePolicy.IGNORE, response.getSchedule().getMisfirePolicy());

        // Check payload
        assertNotNull(response.getPayload());
        assertEquals("https://httpbin.org/post", response.getPayload().get("httpUrl"));
        assertEquals("POST", response.getPayload().get("httpMethod"));
        assertEquals("{\"test\": true}", response.getPayload().get("httpBody"));

        // Check retry policy
        assertNotNull(response.getRetryPolicy());
        assertEquals(2, response.getRetryPolicy().getMaxAttempts());
        assertEquals(BackoffStrategy.FIXED, response.getRetryPolicy().getBackoffStrategy());
        assertEquals(10, response.getRetryPolicy().getBackoffSeconds());
        assertEquals(List.of("CONNECTION_ERROR"), response.getRetryPolicy().getRetryOn());
    }

    @Test
    void toEntity_MinimalRequest_Success() {
        // Given
        CreateJobRequest request = CreateJobRequest.builder()
            .name("Minimal Job")
            .type(JobType.HTTP)
            .schedule(JobScheduleDto.builder()
                .scheduleType(ScheduleType.ONCE)
                .runAt(ZonedDateTime.now().plusMinutes(30))
                .build())
            .payload(HttpJobPayloadDto.builder()
                .httpUrl("https://httpbin.org/get")
                .httpMethod("GET")
                .build())
            .build();

        // When
        Job job = jobMapper.toEntity(request);

        // Then
        assertNotNull(job);
        assertEquals("Minimal Job", job.getName());
        assertEquals(JobType.HTTP, job.getType());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertNull(job.getPriority()); // Should be null if not specified
        assertNull(job.getRetryPolicy()); // Should be null if not specified
        
        assertNotNull(job.getSchedule());
        assertEquals(ScheduleType.ONCE, job.getSchedule().getScheduleType());
        
        assertNotNull(job.getPayload());
        assertTrue(job.getPayload() instanceof HttpJobPayload);
    }

    @Test
    void toResponse_JobWithLatestRun_Success() {
        // Given
        User owner = new User();
        owner.setEmail("test@example.com");

        HttpJobPayload httpPayload = new HttpJobPayload();
        httpPayload.setHttpUrl("https://httpbin.org/get");
        httpPayload.setHttpMethod("GET");

        JobSchedule schedule = new JobSchedule();
        schedule.setScheduleType(ScheduleType.ONCE);
        schedule.setRunAt(Instant.now().minusSeconds(300));

        JobRun latestRun = new JobRun();
        latestRun.setId("run-123");
        latestRun.setScheduledTime(Instant.now().minusSeconds(300));
        latestRun.setStartTime(Instant.now().minusSeconds(295));
        latestRun.setEndTime(Instant.now().minusSeconds(290));
        latestRun.setAttempt(1);
        latestRun.setOutcome(JobOutcome.SUCCESS);
        latestRun.setWorkerId("worker-1");

        Job job = new Job();
        job.setId("job-123");
        job.setName("Job with Run");
        job.setType(JobType.HTTP);
        job.setStatus(JobStatus.SUCCEEDED);
        job.setPriority(JobPriority.LOW);
        job.setOwner(owner);
        job.setPayload(httpPayload);
        job.setSchedule(schedule);
        job.setCreatedAt(Instant.now().minusSeconds(3600));
        job.setLatestRun(latestRun);

        // When
        JobResponse response = jobMapper.toResponse(job);

        // Then
        assertNotNull(response);
        assertEquals("job-123", response.getId());
        assertEquals(JobStatus.SUCCEEDED, response.getStatus());
        
        assertNotNull(response.getLatestRun());
        assertEquals("run-123", response.getLatestRun().getId());
        assertEquals(JobOutcome.SUCCESS, response.getLatestRun().getOutcome());
        assertEquals(1, response.getLatestRun().getAttempt());
        assertEquals("worker-1", response.getLatestRun().getWorkerId());
    }

    @Test
    void toEntity_CronSchedule_Success() {
        // Given
        CreateJobRequest request = CreateJobRequest.builder()
            .name("CRON Job")
            .type(JobType.HTTP)
            .schedule(JobScheduleDto.builder()
                .scheduleType(ScheduleType.CRON)
                .cronExpression("0 0 * * * ?")
                .timezone("America/New_York")
                .misfirePolicy(MisfirePolicy.RESCHEDULE)
                .build())
            .payload(HttpJobPayloadDto.builder()
                .httpUrl("https://httpbin.org/get")
                .httpMethod("GET")
                .build())
            .build();

        // When
        Job job = jobMapper.toEntity(request);

        // Then
        assertNotNull(job.getSchedule());
        assertEquals(ScheduleType.CRON, job.getSchedule().getScheduleType());
        assertEquals("0 0 * * * ?", job.getSchedule().getCronExpression());
        assertEquals(ZoneId.of("America/New_York"), job.getSchedule().getTimezone());
        assertEquals(MisfirePolicy.RESCHEDULE, job.getSchedule().getMisfirePolicy());
    }

    @Test
    void toEntity_IntervalSchedule_Success() {
        // Given
        CreateJobRequest request = CreateJobRequest.builder()
            .name("Interval Job")
            .type(JobType.HTTP)
            .schedule(JobScheduleDto.builder()
                .scheduleType(ScheduleType.INTERVAL)
                .intervalSeconds(3600)
                .misfirePolicy(MisfirePolicy.FIRE_NOW)
                .build())
            .payload(HttpJobPayloadDto.builder()
                .httpUrl("https://httpbin.org/get")
                .httpMethod("GET")
                .build())
            .build();

        // When
        Job job = jobMapper.toEntity(request);

        // Then
        assertNotNull(job.getSchedule());
        assertEquals(ScheduleType.INTERVAL, job.getSchedule().getScheduleType());
        assertEquals(3600, job.getSchedule().getIntervalSeconds());
        assertEquals(MisfirePolicy.FIRE_NOW, job.getSchedule().getMisfirePolicy());
    }

    @Test
    void toEntity_NullPayload_HandledGracefully() {
        // Given
        CreateJobRequest request = CreateJobRequest.builder()
            .name("Job without payload")
            .type(JobType.HTTP)
            .schedule(JobScheduleDto.builder()
                .scheduleType(ScheduleType.ONCE)
                .runAt(ZonedDateTime.now().plusHours(1))
                .build())
            .payload(null)
            .build();

        // When
        Job job = jobMapper.toEntity(request);

        // Then
        assertNotNull(job);
        assertEquals("Job without payload", job.getName());
        assertNull(job.getPayload());
    }

    @Test
    void toResponse_NullOwner_HandledGracefully() {
        // Given
        Job job = new Job();
        job.setId("job-123");
        job.setName("Job without owner");
        job.setType(JobType.HTTP);
        job.setStatus(JobStatus.PENDING);
        job.setOwner(null);
        job.setCreatedAt(Instant.now());

        // When
        JobResponse response = jobMapper.toResponse(job);

        // Then
        assertNotNull(response);
        assertEquals("job-123", response.getId());
        assertNull(response.getOwnerEmail());
    }
}
