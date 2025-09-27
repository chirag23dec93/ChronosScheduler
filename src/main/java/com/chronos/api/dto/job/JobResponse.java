package com.chronos.api.dto.job;

import com.chronos.domain.model.enums.JobPriority;
import com.chronos.domain.model.enums.JobStatus;
import com.chronos.domain.model.enums.JobType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobResponse {
    private String id;
    private String name;
    private JobType type;
    private JobStatus status;
    private JobPriority priority;
    private String ownerEmail;
    private Instant createdAt;
    private JobScheduleDto schedule;
    private JobPayloadDto payload;
    private RetryPolicyDto retryPolicy;
    private JobRunSummaryDto latestRun;
}
