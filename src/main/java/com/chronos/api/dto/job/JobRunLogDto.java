package com.chronos.api.dto.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobRunLogDto {
    private Long id;
    private String runId;
    private Instant timestamp;
    private String level;
    private String message;
    private Map<String, Object> context;
}
