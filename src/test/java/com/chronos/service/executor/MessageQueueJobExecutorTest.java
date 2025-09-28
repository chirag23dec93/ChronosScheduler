package com.chronos.service.executor;

import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.domain.model.payload.MessageQueueJobPayload;
import com.chronos.event.JobLogEvent;
import com.chronos.exception.JobExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageQueueJobExecutorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @InjectMocks
    private MessageQueueJobExecutor messageQueueJobExecutor;

    private Job testJob;
    private JobRun testJobRun;
    private MessageQueueJobPayload messageQueuePayload;

    @BeforeEach
    void setUp() {
        // Setup message queue payload
        messageQueuePayload = new MessageQueueJobPayload();
        messageQueuePayload.setQueueName("test-topic");
        messageQueuePayload.setOperationType("PRODUCE");
        messageQueuePayload.setMessageBody("Test message body");
        messageQueuePayload.setMessageGroupId("group-1");
        messageQueuePayload.setQueueConfig(Map.of(
            "bootstrap.servers", "localhost:9092",
            "key.serializer", "org.apache.kafka.common.serialization.StringSerializer"
        ));
        messageQueuePayload.setBatchSize(10);

        // Setup test job
        testJob = new Job();
        testJob.setId("job-123");
        testJob.setName("Test Message Queue Job");
        testJob.setType(JobType.MESSAGE_QUEUE);
        testJob.setPayload(messageQueuePayload);

        // Setup job run
        testJobRun = new JobRun();
        testJobRun.setId("run-123");
        testJobRun.setJob(testJob);
        testJobRun.setScheduledTime(Instant.now());
        testJobRun.setAttempt(1);

        // Setup mocks
        when(objectMapper.convertValue(any(), eq(MessageQueueJobPayload.class)))
            .thenReturn(messageQueuePayload);
        when(applicationContext.getBean(KafkaTemplate.class)).thenReturn(kafkaTemplate);
    }

    @Test
    void execute_ProduceOperation_Success() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("test-topic"), anyString(), any())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(kafkaTemplate).send(eq("test-topic"), anyString(), any());
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ConsumeOperation_Success() {
        // Given
        messageQueuePayload.setOperationType("CONSUME");

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Kafka consumer simulation completed");
        }));
    }

    @Test
    void execute_PurgeOperation_Success() {
        // Given
        messageQueuePayload.setOperationType("PURGE");

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Kafka topic purge simulation completed");
        }));
    }

    @Test
    void execute_MoveDlqOperation_Success() {
        // Given
        messageQueuePayload.setOperationType("MOVE_DLQ");
        messageQueuePayload.setQueueConfig(Map.of(
            "sourceQueue", "test-topic.dlq",
            "targetQueue", "recovery-topic"
        ));

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Kafka DLQ move simulation completed");
        }));
    }

    @Test
    void execute_InvalidOperation_ThrowsException() {
        // Given
        messageQueuePayload.setOperationType("INVALID_OPERATION");

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> messageQueueJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("Message queue job execution failed"));
        verify(eventPublisher, atLeast(1)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_KafkaException_ThrowsJobExecutionException() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection failed"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> messageQueueJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("Message queue job execution failed"));
        verify(eventPublisher, atLeast(1)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ProduceWithCustomKey_Success() {
        // Given
        messageQueuePayload.setQueueConfig(Map.of(
            "messageKey", "custom-key-123"
        ));
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("test-topic"), eq("custom-key-123"), any())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(kafkaTemplate).send(eq("test-topic"), eq("custom-key-123"), any());
    }

    @Test
    void execute_ProduceWithHeaders_Success() {
        // Given
        messageQueuePayload.setQueueConfig(Map.of(
            "headers", Map.of(
                "source", "chronos-scheduler",
                "version", "1.0"
            )
        ));
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), any());
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ConsumeWithBatchSize_Success() {
        // Given
        messageQueuePayload.setOperationType("CONSUME");
        messageQueuePayload.setBatchSize(5);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("batch size: 5");
        }));
    }

    @Test
    void execute_NullMessageBody_HandledGracefully() {
        // Given
        messageQueuePayload.setMessageBody(null);
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), isNull())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), isNull());
    }

    @Test
    void execute_EmptyQueueConfig_UsesDefaults() {
        // Given
        messageQueuePayload.setQueueConfig(Map.of());
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(kafkaTemplate).send(eq("test-topic"), anyString(), any());
    }

    @Test
    void execute_ProduceWithGroupId_Success() {
        // Given
        messageQueuePayload.setMessageGroupId("test-group");
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(kafkaTemplate).send(anyString(), anyString(), any());
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("group: test-group");
        }));
    }

    @Test
    void execute_ApplicationContextException_ThrowsJobExecutionException() {
        // Given
        when(applicationContext.getBean(KafkaTemplate.class))
            .thenThrow(new RuntimeException("Bean not found"));

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> messageQueueJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("Message queue job execution failed"));
    }

    @Test
    void execute_LoggingEvents_PublishedCorrectly() {
        // Given
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        // When
        assertDoesNotThrow(() -> messageQueueJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Executing message queue operation PRODUCE");
        }));
        
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Message queue operation completed successfully");
        }));
    }
}
