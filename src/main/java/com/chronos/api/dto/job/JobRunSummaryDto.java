package com.chronos.api.dto.job;

import com.chronos.domain.model.enums.JobOutcome;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobRunSummaryDto {
    private String id;
    private Instant scheduledTime;
    private Instant startTime;
    private Instant endTime;
    private Integer attempt;
    private JobOutcome outcome;
    private Integer exitCode;
    private String errorMessage;
    private String workerId;
    private Long durationMs;
}
