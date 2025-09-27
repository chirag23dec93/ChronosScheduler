package com.chronos.api.dto.dlq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DLQEventResponse {
    private Long id;
    private String jobId;
    private String jobName;
    private String lastRunId;
    private String reason;
    private Instant createdAt;
    private Boolean resolved;
}
