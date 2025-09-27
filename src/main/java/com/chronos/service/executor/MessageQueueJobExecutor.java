package com.chronos.service.executor;

import com.chronos.api.dto.job.payload.MessageQueueJobPayload;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.exception.JobExecutionException;
import com.chronos.service.JobExecutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// RabbitMQ imports removed - using Kafka as primary message queue
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;



@Slf4j
@Component
@RequiredArgsConstructor
public class MessageQueueJobExecutor {
    
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private JobExecutorService getJobExecutorService() {
        return applicationContext.getBean(JobExecutorService.class);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute(Job job, JobRun run) {
        try {
            MessageQueueJobPayload payload = objectMapper.convertValue(
                job.getPayload(), MessageQueueJobPayload.class);

            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Executing message queue operation %s on %s", 
                    payload.getOperationType(), payload.getQueueName()));

            String queueType = payload.getQueueConfig().getOrDefault("type", "RABBITMQ").toString();
            
            switch (payload.getOperationType().toUpperCase()) {
                case "PRODUCE":
                    produceMessages(payload, queueType, run);
                    break;
                case "CONSUME":
                    consumeMessages(payload, queueType, run);
                    break;
                case "MOVE_DLQ":
                    moveDLQMessages(payload, queueType, run);
                    break;
                case "PURGE":
                    purgeQueue(payload, queueType, run);
                    break;
                default:
                    throw new JobExecutionException("Unsupported operation type: " + 
                        payload.getOperationType());
            }

            getJobExecutorService().logOutput(run, "INFO", "Message queue operation completed successfully");

        } catch (Exception e) {
            String error = String.format("Message queue job execution failed: %s", e.getMessage());
            getJobExecutorService().logOutput(run, "ERROR", error);
            throw new JobExecutionException(error, e);
        }
    }

    private void produceMessages(MessageQueueJobPayload payload, String queueType, JobRun run) {
        if ("KAFKA".equals(queueType)) {
            // Kafka producer implementation
            try {
                String key = payload.getMessageGroupId() != null ? payload.getMessageGroupId() : "default-key";
                String message = payload.getMessageBody() != null ? payload.getMessageBody() : "Default test message";
                String topicName = payload.getQueueName() != null ? payload.getQueueName() : "default-topic";
                
                kafkaTemplate.send(topicName, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            getJobExecutorService().logOutput(run, "ERROR", 
                                String.format("Failed to produce Kafka message: %s", ex.getMessage()));
                            throw new JobExecutionException("Failed to produce Kafka message", ex);
                        }
                        getJobExecutorService().logOutput(run, "INFO", 
                            String.format("Produced message to Kafka topic %s, partition: %d, offset: %d", 
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()));
                    });
            } catch (Exception e) {
                getJobExecutorService().logOutput(run, "ERROR", 
                    String.format("Kafka produce operation failed: %s", e.getMessage()));
                throw new JobExecutionException("Kafka produce operation failed", e);
            }
        } else {
            // RabbitMQ producer implementation (fallback)
            getJobExecutorService().logOutput(run, "INFO", 
                "RabbitMQ not configured, using Kafka as default message queue");
            // For now, default to Kafka if RabbitMQ is not available
            produceMessages(payload, "KAFKA", run);
        }
    }

    private void consumeMessages(MessageQueueJobPayload payload, String queueType, JobRun run) {
        int batchSize = payload.getBatchSize() != null ? payload.getBatchSize() : 10;
        String topicName = payload.getQueueName() != null ? payload.getQueueName() : "default-topic";

        if ("KAFKA".equals(queueType)) {
            // Kafka consumer implementation
            try {
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Starting Kafka consumer for topic %s with batch size %d", 
                        topicName, batchSize));
                
                // Note: In a real implementation, you would use KafkaConsumer with proper consumer group management
                // For this demo, we'll simulate consumption
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Kafka consumer simulation completed for topic %s. " +
                        "In production, this would consume %d messages from the topic.", 
                        topicName, batchSize));
                        
            } catch (Exception e) {
                getJobExecutorService().logOutput(run, "ERROR", 
                    String.format("Kafka consume operation failed: %s", e.getMessage()));
                throw new JobExecutionException("Kafka consume operation failed", e);
            }
        } else {
            // RabbitMQ consumer implementation (fallback)
            getJobExecutorService().logOutput(run, "INFO", 
                "RabbitMQ not configured, using Kafka as default message queue");
            // For now, default to Kafka if RabbitMQ is not available
            consumeMessages(payload, "KAFKA", run);
        }
    }

    private void moveDLQMessages(MessageQueueJobPayload payload, String queueType, JobRun run) {
        String topicName = payload.getQueueName() != null ? payload.getQueueName() : "default-topic";
        String dlqTopicName = topicName + ".dlq";
        
        if (payload.getQueueConfig() == null || !payload.getQueueConfig().containsKey("targetQueue")) {
            throw new JobExecutionException("Target queue is required in queueConfig for MOVE_DLQ operations");
        }
        
        String targetTopic = payload.getQueueConfig().get("targetQueue").toString();

        if ("KAFKA".equals(queueType)) {
            // Kafka DLQ implementation
            try {
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Starting Kafka DLQ move from %s to %s", dlqTopicName, targetTopic));
                
                // Note: In a real implementation, you would use Kafka Admin Client and Consumer/Producer
                // to move messages from DLQ topic to target topic
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Kafka DLQ move simulation completed from %s to %s. " +
                        "In production, this would move messages between topics.", 
                        dlqTopicName, targetTopic));
                        
            } catch (Exception e) {
                getJobExecutorService().logOutput(run, "ERROR", 
                    String.format("Kafka DLQ move operation failed: %s", e.getMessage()));
                throw new JobExecutionException("Kafka DLQ move operation failed", e);
            }
        } else {
            // RabbitMQ DLQ implementation (fallback)
            getJobExecutorService().logOutput(run, "INFO", 
                "RabbitMQ not configured, using Kafka as default message queue");
            // For now, default to Kafka if RabbitMQ is not available
            moveDLQMessages(payload, "KAFKA", run);
        }
    }

    private void purgeQueue(MessageQueueJobPayload payload, String queueType, JobRun run) {
        String topicName = payload.getQueueName() != null ? payload.getQueueName() : "default-topic";
        
        if ("KAFKA".equals(queueType)) {
            // Kafka purge implementation
            try {
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Starting Kafka topic purge for %s", topicName));
                
                // Note: In a real implementation, you would use Kafka Admin Client
                // to delete and recreate the topic, or use retention policies
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Kafka topic purge simulation completed for %s. " +
                        "In production, this would purge all messages from the topic.", 
                        topicName));
                        
            } catch (Exception e) {
                getJobExecutorService().logOutput(run, "ERROR", 
                    String.format("Kafka purge operation failed: %s", e.getMessage()));
                throw new JobExecutionException("Kafka purge operation failed", e);
            }
        } else {
            // RabbitMQ purge implementation (fallback)
            getJobExecutorService().logOutput(run, "INFO", 
                "RabbitMQ not configured, using Kafka as default message queue");
            // For now, default to Kafka if RabbitMQ is not available
            purgeQueue(payload, "KAFKA", run);
        }
    }
}
