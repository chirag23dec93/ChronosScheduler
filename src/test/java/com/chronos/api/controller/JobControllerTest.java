package com.chronos.api.controller;

import com.chronos.api.dto.job.CreateJobRequest;
import com.chronos.api.dto.job.JobResponse;
import com.chronos.api.dto.job.JobScheduleDto;
import com.chronos.api.dto.job.payload.HttpJobPayloadDto;
import com.chronos.api.mapper.JobMapper;
import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.domain.model.payload.HttpJobPayload;
import com.chronos.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobService jobService;

    @MockBean
    private JobMapper jobMapper;

    private Job testJob;
    private JobResponse jobResponse;
    private CreateJobRequest createJobRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        User owner = new User();
        owner.setEmail("test@example.com");

        // Setup HTTP payload
        HttpJobPayload httpPayload = new HttpJobPayload();
        httpPayload.setHttpUrl("https://httpbin.org/get");
        httpPayload.setHttpMethod("GET");

        // Setup job schedule
        JobSchedule schedule = new JobSchedule();
        schedule.setScheduleType(ScheduleType.ONCE);
        schedule.setRunAt(Instant.now().plusSeconds(3600));

        // Setup test job
        testJob = new Job();
        testJob.setId("job-123");
        testJob.setName("Test HTTP Job");
        testJob.setType(JobType.HTTP);
        testJob.setStatus(JobStatus.SCHEDULED);
        testJob.setPriority(JobPriority.MEDIUM);
        testJob.setOwner(owner);
        testJob.setPayload(httpPayload);
        testJob.setSchedule(schedule);
        testJob.setCreatedAt(Instant.now());

        // Setup job response
        jobResponse = JobResponse.builder()
            .id("job-123")
            .name("Test HTTP Job")
            .type(JobType.HTTP)
            .status(JobStatus.SCHEDULED)
            .priority(JobPriority.MEDIUM)
            .ownerEmail("test@example.com")
            .createdAt(Instant.now())
            .build();

        // Setup create job request
        createJobRequest = CreateJobRequest.builder()
            .name("Test HTTP Job")
            .type(JobType.HTTP)
            .priority(JobPriority.MEDIUM)
            .schedule(JobScheduleDto.builder()
                .scheduleType(ScheduleType.ONCE)
                .runAt(ZonedDateTime.now().plusHours(1))
                .build())
            .payload(HttpJobPayloadDto.builder()
                .httpUrl("https://httpbin.org/get")
                .httpMethod("GET")
                .build())
            .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createJob_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        when(jobMapper.toEntity(any(CreateJobRequest.class))).thenReturn(testJob);
        when(jobService.createJob(any(Job.class))).thenReturn(testJob);
        when(jobMapper.toResponse(any(Job.class))).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(post("/api/jobs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createJobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("job-123"))
                .andExpect(jsonPath("$.name").value("Test HTTP Job"))
                .andExpect(jsonPath("$.type").value("HTTP"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.ownerEmail").value("test@example.com"));

        verify(jobService).createJob(any(Job.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createJob_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        CreateJobRequest invalidRequest = CreateJobRequest.builder()
            .name("") // Invalid empty name
            .type(JobType.HTTP)
            .build();

        // When & Then
        mockMvc.perform(post("/api/jobs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(jobService, never()).createJob(any(Job.class));
    }

    @Test
    void createJob_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/jobs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createJobRequest)))
                .andExpect(status().isUnauthorized());

        verify(jobService, never()).createJob(any(Job.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getJob_ExistingJob_ReturnsJob() throws Exception {
        // Given
        when(jobService.getJobById("job-123")).thenReturn(testJob);
        when(jobMapper.toResponse(testJob)).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(get("/api/jobs/job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("job-123"))
                .andExpect(jsonPath("$.name").value("Test HTTP Job"))
                .andExpect(jsonPath("$.type").value("HTTP"));

        verify(jobService).getJobById("job-123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getJob_NonExistentJob_ReturnsNotFound() throws Exception {
        // Given
        when(jobService.getJobById("nonexistent"))
            .thenThrow(new RuntimeException("Job not found"));

        // When & Then
        mockMvc.perform(get("/api/jobs/nonexistent"))
                .andExpect(status().isInternalServerError());

        verify(jobService).getJobById("nonexistent");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getAllJobs_ReturnsPagedJobs() throws Exception {
        // Given
        List<Job> jobs = List.of(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs);
        when(jobService.getAllJobs(any(Pageable.class))).thenReturn(jobPage);
        when(jobMapper.toResponse(testJob)).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(get("/api/jobs")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("job-123"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(jobService).getAllJobs(any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void pauseJob_ExistingJob_ReturnsUpdatedJob() throws Exception {
        // Given
        testJob.setStatus(JobStatus.PAUSED);
        jobResponse = jobResponse.toBuilder().status(JobStatus.PAUSED).build();
        
        when(jobService.pauseJob("job-123")).thenReturn(testJob);
        when(jobMapper.toResponse(testJob)).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(post("/api/jobs/job-123:pause")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("job-123"))
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(jobService).pauseJob("job-123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void resumeJob_ExistingJob_ReturnsUpdatedJob() throws Exception {
        // Given
        when(jobService.resumeJob("job-123")).thenReturn(testJob);
        when(jobMapper.toResponse(testJob)).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(post("/api/jobs/job-123:resume")
                .with(csrf()))
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.id").value("job-123"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(jobService).resumeJob("job-123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void cancelJob_ExistingJob_ReturnsUpdatedJob() throws Exception {
        // Given
        testJob.setStatus(JobStatus.CANCELLED);
        jobResponse = jobResponse.toBuilder().status(JobStatus.CANCELLED).build();
        
        when(jobService.cancelJob("job-123")).thenReturn(testJob);
        when(jobMapper.toResponse(testJob)).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(post("/api/jobs/job-123:cancel")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("job-123"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(jobService).cancelJob("job-123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteJob_ExistingJob_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(jobService).deleteJob("job-123");

        // When & Then
        mockMvc.perform(delete("/api/jobs/job-123")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(jobService).deleteJob("job-123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getJobRuns_ExistingJob_ReturnsRuns() throws Exception {
        // Given
        JobRun jobRun = new JobRun();
        jobRun.setId("run-123");
        jobRun.setJob(testJob);
        jobRun.setScheduledTime(Instant.now());
        jobRun.setAttempt(1);
        jobRun.setOutcome(JobOutcome.SUCCESS);

        List<JobRun> runs = List.of(jobRun);
        Page<JobRun> runPage = new PageImpl<>(runs);
        
        when(jobService.getJobRuns(eq("job-123"), any(Pageable.class))).thenReturn(runPage);

        // When & Then
        mockMvc.perform(get("/api/jobs/job-123/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("run-123"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(jobService).getJobRuns(eq("job-123"), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getJobRunLogs_ExistingRun_ReturnsLogs() throws Exception {
        // Given
        JobRunLog log = new JobRunLog();
        log.setId(1L);
        log.setRunId("run-123");
        log.setTimestamp(Instant.now());
        log.setLevel("INFO");
        log.setMessage("Job started");

        List<JobRunLog> logs = List.of(log);
        Page<JobRunLog> logPage = new PageImpl<>(logs);
        
        when(jobService.getJobRunLogs(eq("job-123"), eq("run-123"), any(Pageable.class)))
            .thenReturn(logPage);

        // When & Then
        mockMvc.perform(get("/api/jobs/job-123/runs/run-123/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].message").value("Job started"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(jobService).getJobRunLogs(eq("job-123"), eq("run-123"), any(Pageable.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createJob_WithValidationErrors_ReturnsBadRequest() throws Exception {
        // Given
        CreateJobRequest invalidRequest = CreateJobRequest.builder()
            .name(null) // Null name should fail validation
            .type(null) // Null type should fail validation
            .build();

        // When & Then
        mockMvc.perform(post("/api/jobs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(jobService, never()).createJob(any(Job.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getAllJobs_WithFilters_ReturnsFilteredJobs() throws Exception {
        // Given
        List<Job> jobs = List.of(testJob);
        Page<Job> jobPage = new PageImpl<>(jobs);
        when(jobService.getAllJobs(any(Pageable.class))).thenReturn(jobPage);
        when(jobMapper.toResponse(testJob)).thenReturn(jobResponse);

        // When & Then
        mockMvc.perform(get("/api/jobs")
                .param("type", "HTTP")
                .param("status", "SCHEDULED")
                .param("priority", "MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(jobService).getAllJobs(any(Pageable.class));
    }
}
