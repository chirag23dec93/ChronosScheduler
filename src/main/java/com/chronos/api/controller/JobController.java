package com.chronos.api.controller;

import com.chronos.api.dto.job.*;
import com.chronos.domain.model.enums.JobStatus;
import com.chronos.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.net.URI;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job management APIs")
@SecurityRequirement(name = "bearerAuth")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "Create a new job")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> createJob(
            @RequestBody @Valid CreateJobRequest request
    ) {
        JobResponse job = jobService.createJob(request);
        return ResponseEntity
                .created(URI.create("/api/v1/jobs/" + job.getId()))
                .body(job);
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job details")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobService.getJob(jobId));
    }

    @GetMapping
    @Operation(summary = "List jobs with filters")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<JobResponse>> getJobs(
            @Parameter(description = "Filter by job status")
            @RequestParam(required = false) JobStatus status,
            
            @Parameter(description = "Filter by job name containing")
            @RequestParam(required = false) String nameContains,
            
            @Parameter(description = "Filter by next run time from")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant nextRunFrom,
            
            @Parameter(description = "Filter by next run time to")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant nextRunTo,
            
            Pageable pageable
    ) {
        return ResponseEntity.ok(jobService.getJobs(status, nameContains, 
                nextRunFrom, nextRunTo, pageable));
    }

    @PutMapping("/{jobId}")
    @Operation(summary = "Update job configuration")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable String jobId,
            @RequestBody @Valid CreateJobRequest request
    ) {
        return ResponseEntity.ok(jobService.updateJob(jobId, request));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Delete a job")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> deleteJob(
            @PathVariable String jobId
    ) {
        jobService.deleteJob(jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}:schedule")
    @Operation(summary = "Schedule a job for execution")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> scheduleJob(
            @PathVariable String jobId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant runAt
    ) {
        return ResponseEntity.ok(jobService.scheduleJob(jobId, runAt));
    }

    @PostMapping("/{jobId}:pause")
    @Operation(summary = "Pause a job")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> pauseJob(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobService.pauseJob(jobId));
    }

    @PostMapping("/{jobId}:resume")
    @Operation(summary = "Resume a paused job")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> resumeJob(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobService.resumeJob(jobId));
    }

    @PostMapping("/{jobId}:cancel")
    @Operation(summary = "Cancel a job")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> cancelJob(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobService.cancelJob(jobId));
    }

    @PostMapping("/{jobId}:run-now")
    @Operation(summary = "Trigger immediate job execution")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobResponse> triggerJobNow(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(jobService.triggerJobNow(jobId));
    }

    @GetMapping("/{jobId}/runs")
    @Operation(summary = "Get job execution history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<JobRunSummaryDto>> getJobRuns(
            @PathVariable String jobId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(jobService.getJobRuns(jobId, pageable));
    }

    @GetMapping("/{jobId}/runs/{runId}")
    @Operation(summary = "Get job run details")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<JobRunSummaryDto> getJobRun(
            @PathVariable String jobId,
            @PathVariable String runId
    ) {
        return ResponseEntity.ok(jobService.getJobRun(jobId, runId));
    }

    @GetMapping("/{jobId}/runs/{runId}/logs")
    @Operation(summary = "Get job run logs")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Page<JobRunLogDto>> getJobRunLogs(
            @PathVariable String jobId,
            @PathVariable String runId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(jobService.getJobRunLogs(jobId, runId, pageable));
    }
}
