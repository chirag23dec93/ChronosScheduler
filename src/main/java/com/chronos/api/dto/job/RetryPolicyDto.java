package com.chronos.api.dto.job;

import com.chronos.domain.model.enums.BackoffStrategy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RetryPolicyDto {
    @Min(value = 0, message = "Max attempts must be non-negative")
    @Max(value = 10, message = "Max attempts cannot exceed 10")
    private Integer maxAttempts;

    @NotNull(message = "Backoff strategy is required")
    private BackoffStrategy backoffStrategy;

    @Min(value = 1, message = "Backoff seconds must be at least 1")
    @Max(value = 3600, message = "Backoff seconds cannot exceed 3600")
    private Integer backoffSeconds;

    private String[] retryOn;
}
