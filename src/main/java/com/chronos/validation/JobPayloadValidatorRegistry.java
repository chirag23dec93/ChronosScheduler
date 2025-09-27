package com.chronos.validation;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JobPayloadValidatorRegistry {
    private final Map<JobType, JobPayloadValidator> validators;

    public JobPayloadValidatorRegistry(List<JobPayloadValidator> validators) {
        Map<JobType, JobPayloadValidator> validatorMap = new HashMap<>();
        for (JobPayloadValidator validator : validators) {
            for (JobType type : JobType.values()) {
                if (validator.supports(type)) {
                    validatorMap.put(type, validator);
                    log.info("Registered validator {} for job type {}", validator.getClass().getSimpleName(), type);
                }
            }
        }
        this.validators = validatorMap;
        log.info("Registered {} job payload validators", validatorMap.size());
    }

    /**
     * Validate a job payload for a specific job type
     * @param type The job type
     * @param payload The payload to validate
     * @throws IllegalArgumentException if validation fails or no validator is found
     */
    public void validate(JobType type, JobPayload payload) {
        JobPayloadValidator validator = validators.get(type);
        if (validator == null) {
            throw new IllegalArgumentException("No validator found for job type: " + type);
        }
        validator.validate(payload);
    }

    /**
     * Check if a validator exists for a job type
     * @param type The job type to check
     * @return true if a validator exists
     */
    public boolean hasValidatorFor(JobType type) {
        return validators.containsKey(type);
    }
}
