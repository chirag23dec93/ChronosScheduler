package com.chronos.service.executor;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.payload.DbToKafkaJobPayload;
import com.chronos.domain.model.payload.DatabaseJobPayload;
import com.chronos.exception.JobExecutionException;
import com.chronos.service.JobExecutorService;
import com.chronos.config.DatabaseJobDataSourceFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbToKafkaJobExecutor {
    
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DatabaseJobDataSourceFactory dataSourceFactory;
    
    private JobExecutorService getJobExecutorService() {
        return applicationContext.getBean(JobExecutorService.class);
    }
    
    private JdbcTemplate createJdbcTemplate(DbToKafkaJobPayload payload) {
        // Create a DatabaseJobPayload for compatibility with existing factory
        DatabaseJobPayload dbPayload = new DatabaseJobPayload();
        dbPayload.setDatabaseUrl(payload.getDatabaseUrl());
        dbPayload.setQuery(payload.getQuery());
        dbPayload.setParameters(payload.getQueryParameters());
        dbPayload.setQueryTimeoutSeconds(payload.getQueryTimeoutSeconds());
        
        return dataSourceFactory.createJdbcTemplate(dbPayload);
    }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute(Job job, JobRun run) {
        try {
            DbToKafkaJobPayload payload = objectMapper.convertValue(
                job.getPayload(), DbToKafkaJobPayload.class);
            
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Starting DB to Kafka streaming for topic %s", payload.getKafkaTopic()));
            
            // Create database connection
            JdbcTemplate jdbcTemplate = createJdbcTemplate(payload);
            
            // Build query with offset handling
            String finalQuery = buildQueryWithOffset(payload);
            
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Executing query: %s", finalQuery));
            
            // Execute streaming process
            StreamingResult result = executeStreamingQuery(jdbcTemplate, finalQuery, payload, run);
            
            // Update offset for next execution
            updateLastProcessedValue(job, payload, result.getLastProcessedValue());
            
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("DB to Kafka streaming completed. Processed: %d records, Sent: %d messages, Errors: %d", 
                    result.getProcessedCount(), result.getSentCount(), result.getErrorCount()));
            
        } catch (Exception e) {
            String error = String.format("DB to Kafka job execution failed: %s", e.getMessage());
            getJobExecutorService().logOutput(run, "ERROR", error);
            throw new JobExecutionException(error, e);
        }
    }
    
    private String buildQueryWithOffset(DbToKafkaJobPayload payload) {
        String baseQuery = payload.getQuery().trim();
        
        // Add WHERE clause for offset if specified
        if (payload.getOffsetField() != null && payload.getLastProcessedValue() != null) {
            String whereClause = String.format(" WHERE %s > '%s'", 
                payload.getOffsetField(), payload.getLastProcessedValue());
            
            if (baseQuery.toUpperCase().contains("WHERE")) {
                // Replace WHERE with WHERE ... AND
                baseQuery = baseQuery.replaceFirst("(?i)WHERE", whereClause + " AND");
            } else {
                // Add WHERE clause
                baseQuery += whereClause;
            }
        }
        
        // Add ORDER BY for consistent processing
        if (payload.getOffsetField() != null) {
            if (!baseQuery.toUpperCase().contains("ORDER BY")) {
                baseQuery += String.format(" ORDER BY %s ASC", payload.getOffsetField());
            }
        }
        
        // Add LIMIT for batch processing (only if not already present)
        if (payload.getMaxRecords() != null && !baseQuery.toUpperCase().contains("LIMIT")) {
            baseQuery += String.format(" LIMIT %d", payload.getMaxRecords());
        }
        
        return baseQuery;
    }
    
    private StreamingResult executeStreamingQuery(JdbcTemplate jdbcTemplate, String query, 
                                                 DbToKafkaJobPayload payload, JobRun run) {
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger sentCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong lastProcessedValue = new AtomicLong(0);
        
        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>();
        
        try {
            jdbcTemplate.query(query, (ResultSet rs) -> {
                try {
                    // Convert ResultSet row to Map
                    Map<String, Object> record = resultSetToMap(rs);
                    processedCount.incrementAndGet();
                    
                    // Track offset value
                    if (payload.getOffsetField() != null && record.containsKey(payload.getOffsetField())) {
                        Object offsetValue = record.get(payload.getOffsetField());
                        if (offsetValue instanceof Number) {
                            lastProcessedValue.set(((Number) offsetValue).longValue());
                        }
                    }
                    
                    // Transform record
                    Map<String, Object> transformedRecord = transformRecord(record, payload, run);
                    
                    // Send to Kafka
                    CompletableFuture<SendResult<String, String>> future = sendToKafka(
                        transformedRecord, payload, run);
                    
                    if (future != null) {
                        futures.add(future);
                        sentCount.incrementAndGet();
                    }
                    
                    // Process in batches to avoid memory issues
                    if (futures.size() >= payload.getBatchSize()) {
                        waitForBatch(futures, errorCount, payload.isSkipOnError());
                        futures.clear();
                        
                        getJobExecutorService().logOutput(run, "INFO", 
                            String.format("Processed batch: %d records", processedCount.get()));
                    }
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    getJobExecutorService().logOutput(run, "ERROR", 
                        String.format("Error processing record: %s", e.getMessage()));
                    
                    if (!payload.isSkipOnError()) {
                        throw new JobExecutionException("Record processing failed", e);
                    }
                }
            });
            
            // Wait for remaining futures
            if (!futures.isEmpty()) {
                waitForBatch(futures, errorCount, payload.isSkipOnError());
            }
            
        } catch (Exception e) {
            throw new JobExecutionException("Query execution failed", e);
        }
        
        return new StreamingResult(processedCount.get(), sentCount.get(), 
                                 errorCount.get(), String.valueOf(lastProcessedValue.get()));
    }
    
    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> record = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Object value = rs.getObject(i);
            record.put(columnName, value);
        }
        
        return record;
    }
    
    private Map<String, Object> transformRecord(Map<String, Object> record, 
                                              DbToKafkaJobPayload payload, JobRun run) {
        Map<String, Object> transformed = new HashMap<>(record);
        
        // Apply field mappings
        if (payload.getFieldMappings() != null && !payload.getFieldMappings().isEmpty()) {
            Map<String, Object> mapped = new HashMap<>();
            for (Map.Entry<String, String> mapping : payload.getFieldMappings().entrySet()) {
                String dbField = mapping.getKey();
                String kafkaField = mapping.getValue();
                if (record.containsKey(dbField)) {
                    mapped.put(kafkaField, record.get(dbField));
                }
            }
            transformed = mapped;
        }
        
        // Exclude fields
        if (payload.getExcludeFields() != null) {
            payload.getExcludeFields().forEach(transformed::remove);
        }
        
        // Add metadata if requested
        if (payload.isIncludeMetadata()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("job_id", run.getJob().getId());
            metadata.put("job_name", run.getJob().getName());
            metadata.put("run_id", run.getId());
            metadata.put("processed_at", Instant.now().toString());
            metadata.put("source_table", extractTableFromQuery(payload.getQuery()));
            
            transformed.put("_metadata", metadata);
        }
        
        return transformed;
    }
    
    private CompletableFuture<SendResult<String, String>> sendToKafka(
            Map<String, Object> record, DbToKafkaJobPayload payload, JobRun run) {
        
        try {
            // Determine Kafka key
            String key = "default-key";
            if (payload.getKafkaKeyField() != null && record.containsKey(payload.getKafkaKeyField())) {
                key = String.valueOf(record.get(payload.getKafkaKeyField()));
            }
            
            // Convert record to JSON
            String message = objectMapper.writeValueAsString(record);
            
            // Send to Kafka with retry logic
            return kafkaTemplate.send(payload.getKafkaTopic(), key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        getJobExecutorService().logOutput(run, "ERROR", 
                            String.format("Failed to send message to Kafka: %s", ex.getMessage()));
                        
                        // Send to DLQ if configured
                        if (payload.getDeadLetterTopic() != null) {
                            sendToDeadLetterQueue(record, payload, run, ex.getMessage());
                        }
                    } else {
                        log.debug("Message sent to Kafka topic {} partition {} offset {}", 
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                });
                
        } catch (Exception e) {
            getJobExecutorService().logOutput(run, "ERROR", 
                String.format("Error preparing message for Kafka: %s", e.getMessage()));
            return null;
        }
    }
    
    private void sendToDeadLetterQueue(Map<String, Object> record, DbToKafkaJobPayload payload, 
                                     JobRun run, String errorMessage) {
        try {
            // Add error information to record
            Map<String, Object> dlqRecord = new HashMap<>(record);
            dlqRecord.put("_error", errorMessage);
            dlqRecord.put("_failed_at", Instant.now().toString());
            dlqRecord.put("_original_topic", payload.getKafkaTopic());
            
            String dlqMessage = objectMapper.writeValueAsString(dlqRecord);
            kafkaTemplate.send(payload.getDeadLetterTopic(), dlqMessage);
            
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Sent failed message to DLQ: %s", payload.getDeadLetterTopic()));
                
        } catch (Exception e) {
            getJobExecutorService().logOutput(run, "ERROR", 
                String.format("Failed to send message to DLQ: %s", e.getMessage()));
        }
    }
    
    private void waitForBatch(List<CompletableFuture<SendResult<String, String>>> futures, 
                            AtomicInteger errorCount, boolean skipOnError) {
        for (CompletableFuture<SendResult<String, String>> future : futures) {
            try {
                future.get(); // Wait for completion
            } catch (Exception e) {
                errorCount.incrementAndGet();
                if (!skipOnError) {
                    throw new JobExecutionException("Kafka send failed", e);
                }
            }
        }
    }
    
    private void updateLastProcessedValue(Job job, DbToKafkaJobPayload payload, String lastValue) {
        // In a real implementation, you would update the job payload in the database
        // This ensures the next execution starts from where this one left off
        if (payload.getOffsetField() != null && lastValue != null) {
            payload.setLastProcessedValue(lastValue);
            // TODO: Persist this change to the database
            log.info("Updated last processed value to: {}", lastValue);
        }
    }
    
    private String extractTableFromQuery(String query) {
        // Simple table name extraction from SELECT query
        try {
            String upperQuery = query.toUpperCase();
            int fromIndex = upperQuery.indexOf("FROM");
            if (fromIndex != -1) {
                String afterFrom = query.substring(fromIndex + 4).trim();
                String[] parts = afterFrom.split("\\s+");
                if (parts.length > 0) {
                    return parts[0];
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract table name from query: {}", query);
        }
        return "unknown";
    }
    
    // Helper class to track streaming results
    private static class StreamingResult {
        private final int processedCount;
        private final int sentCount;
        private final int errorCount;
        private final String lastProcessedValue;
        
        public StreamingResult(int processedCount, int sentCount, int errorCount, String lastProcessedValue) {
            this.processedCount = processedCount;
            this.sentCount = sentCount;
            this.errorCount = errorCount;
            this.lastProcessedValue = lastProcessedValue;
        }
        
        public int getProcessedCount() { return processedCount; }
        public int getSentCount() { return sentCount; }
        public int getErrorCount() { return errorCount; }
        public String getLastProcessedValue() { return lastProcessedValue; }
    }
}
