package com.chronos.api.dto.job.payload;

import com.chronos.api.dto.job.JobPayloadDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatabaseJobPayloadDto extends JobPayloadDto {
    @NotBlank(message = "Query is required")
    private String query;

    @NotBlank(message = "Database URL is required")
    private String databaseUrl;

    @NotNull(message = "Query parameters must not be null")
    private Map<String, Object> parameters;

    private String transactionIsolation;
    private Integer queryTimeoutSeconds;
    private Integer maxRows;
    private Boolean readOnly;
}
