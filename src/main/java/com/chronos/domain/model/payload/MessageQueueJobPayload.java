package com.chronos.domain.model.payload;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@DiscriminatorValue("MESSAGE_QUEUE")
@JsonTypeName("MESSAGE_QUEUE")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class MessageQueueJobPayload extends JobPayload {
    
    @NotBlank(message = "Queue name is required")
    @Column(name = "queue_name")
    private String queueName;

    @NotBlank(message = "Operation type is required")
    @Column(name = "operation_type")
    private String operationType;

    @Column(name = "message_body", columnDefinition = "text")
    private String messageBody;

    @Column(name = "message_group_id")
    private String messageGroupId;

    @Column(name = "message_deduplication_id")
    private String messageDeduplicationId;

    @Column(name = "message_attributes", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> messageAttributes = new HashMap<>();

    @Column(name = "queue_config", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> queueConfig = new HashMap<>();

    @Column(name = "batch_size")
    private Integer batchSize;

    @Column(name = "visibility_timeout_seconds")
    private Integer visibilityTimeoutSeconds;

    // Legacy fields for backward compatibility
    @Column(name = "routing_key")
    private String routingKey;

    @Column(name = "exchange")
    private String exchange;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "time_to_live_seconds")
    private Integer timeToLiveSeconds;

    @Column(name = "persistent")
    private Boolean persistent;

    @Column(name = "headers", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> headers = new HashMap<>();

    @Override
    public void validate(JobType type) {
        if (type != JobType.MESSAGE_QUEUE) {
            throw new IllegalArgumentException("Invalid job type for MessageQueueJobPayload: " + type);
        }

        if (queueName == null || queueName.isBlank()) {
            throw new IllegalArgumentException("Queue name is required for MESSAGE_QUEUE jobs");
        }

        if (operationType == null || operationType.isBlank()) {
            throw new IllegalArgumentException("Operation type is required for MESSAGE_QUEUE jobs");
        }

        // Validate operation type
        String opType = operationType.toUpperCase();
        if (!opType.equals("PRODUCE") && !opType.equals("CONSUME") && 
            !opType.equals("MOVE_DLQ") && !opType.equals("PURGE")) {
            throw new IllegalArgumentException("Invalid operation type: " + operationType + 
                ". Valid types are: PRODUCE, CONSUME, MOVE_DLQ, PURGE");
        }

        // For PRODUCE operations, message body is required
        if ("PRODUCE".equals(opType) && (messageBody == null || messageBody.isBlank())) {
            throw new IllegalArgumentException("Message body is required for PRODUCE operations");
        }

        // For MOVE_DLQ operations, target queue is required in config
        if ("MOVE_DLQ".equals(opType) && (queueConfig == null || !queueConfig.containsKey("targetQueue"))) {
            throw new IllegalArgumentException("Target queue is required in queueConfig for MOVE_DLQ operations");
        }

        if (timeToLiveSeconds != null && timeToLiveSeconds <= 0) {
            throw new IllegalArgumentException("Time to live must be positive");
        }

        if (priority != null && (priority < 0 || priority > 9)) {
            throw new IllegalArgumentException("Priority must be between 0 and 9");
        }

        if (batchSize != null && batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        if (visibilityTimeoutSeconds != null && visibilityTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Visibility timeout must be positive");
        }
    }
}
