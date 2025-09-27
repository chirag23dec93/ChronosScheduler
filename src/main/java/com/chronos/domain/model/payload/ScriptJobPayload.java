package com.chronos.domain.model.payload;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class ScriptJobPayload extends JobPayload {
    @NotBlank(message = "Script content is required")
    private String script;

    @Override
    public void validate(JobType type) {
        if (type != JobType.SCRIPT) {
            throw new IllegalArgumentException("Invalid job type for ScriptJobPayload: " + type);
        }
        if (script == null || script.isBlank()) {
            throw new IllegalArgumentException("Script content is required for SCRIPT jobs");
        }
    }
}
