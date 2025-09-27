package com.chronos.api.dto.job.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for message queue job payload configuration.
 * Supports operations on message queues like RabbitMQ and Kafka.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageQueueJobPayload {

    /**
     * Name of the queue to operate on
     */
    @NotBlank(message = "Queue name is required")
    private String queueName;

    /**
     * Type of operation to perform (PRODUCE, CONSUME, MOVE_DLQ, PURGE)
     */
    @NotBlank(message = "Operation type is required")
    private String operationType;

    /**
     * Message body for PRODUCE operations
     */
    private String messageBody;

    /**
     * Optional message group ID for ordered message processing
     */
    private String messageGroupId;

    /**
     * Optional message deduplication ID
     */
    private String messageDeduplicationId;

    /**
     * Additional message attributes/headers
     */
    private Map<String, Object> messageAttributes = new HashMap<>();

    /**
     * Queue-specific configuration (type: RABBITMQ/KAFKA, etc.)
     */
    @NotNull(message = "Queue configuration is required")
    private Map<String, Object> queueConfig = new HashMap<>();

    /**
     * Number of messages to process in batch operations
     */
    private Integer batchSize;

    /**
     * Visibility timeout in seconds for consume operations
     */
    private Integer visibilityTimeoutSeconds;
}
