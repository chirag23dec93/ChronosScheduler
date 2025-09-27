package com.chronos.api.dto.job.payload;

import com.chronos.api.dto.job.JobPayloadDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageQueueJobPayloadDto extends JobPayloadDto {
    
    @NotBlank(message = "Queue name is required")
    private String queueName;

    @NotBlank(message = "Operation type is required")
    private String operationType;

    private String messageBody;

    private String messageGroupId;

    private String messageDeduplicationId;

    @NotNull(message = "Message attributes must not be null")
    private Map<String, Object> messageAttributes = new HashMap<>();

    @NotNull(message = "Queue configuration must not be null")
    private Map<String, Object> queueConfig = new HashMap<>();

    private Integer batchSize;

    private Integer visibilityTimeoutSeconds;

    // Legacy fields for backward compatibility
    private String routingKey;
    
    private String exchange;
    
    private Integer priority;
    
    private Integer timeToLiveSeconds;
    
    private Boolean persistent;
    
    @NotNull(message = "Message headers must not be null")
    private Map<String, Object> headers = new HashMap<>();
}
