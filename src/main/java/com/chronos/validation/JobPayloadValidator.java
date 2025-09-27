package com.chronos.validation;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;

public interface JobPayloadValidator {
    /**
     * Validate the job payload for a specific job type
     * @param payload The payload to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validate(JobPayload payload);

    /**
     * Check if this validator supports the given job type
     * @param type The job type to check
     * @return true if this validator supports the job type
     */
    boolean supports(JobType type);
}
