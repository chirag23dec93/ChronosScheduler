package com.chronos.api.dto.job;

import com.chronos.domain.model.enums.JobPriority;
import com.chronos.domain.model.enums.JobType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateJobRequest {
    @NotBlank(message = "Job name is required")
    private String name;

    @NotNull(message = "Job type is required")
    private JobType type;

    @NotNull(message = "Job priority is required")
    private JobPriority priority;

    @Valid
    @NotNull(message = "Schedule is required")
    private JobScheduleDto schedule;

    @Valid
    @NotNull(message = "Payload is required")
    private JobPayloadDto payload;

    @Valid
    private RetryPolicyDto retryPolicy;
}
