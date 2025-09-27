package com.chronos.validation;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import com.chronos.domain.model.payload.FileSystemJobPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileSystemJobPayloadValidator implements JobPayloadValidator {

    @Override
    public boolean supports(JobType type) {
        return type == JobType.FILE_SYSTEM;
    }

    @Override
    public void validate(JobPayload payload) {
        if (!(payload instanceof FileSystemJobPayload fsPayload)) {
            throw new IllegalArgumentException("Invalid payload type for FILE_SYSTEM job");
        }
        fsPayload.validate(JobType.FILE_SYSTEM);
    }
}
