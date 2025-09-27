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
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@DiscriminatorValue("DB_TO_KAFKA")
@JsonTypeName("DB_TO_KAFKA")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DbToKafkaJobPayload extends JobPayload {
    
    // Database Configuration
    @NotBlank(message = "Database URL is required")
    @Column(name = "database_url")
    private String databaseUrl;
    
    @NotBlank(message = "SQL query is required")
    @Column(name = "sql_query", columnDefinition = "text")
    private String query;
    
    @Column(name = "query_parameters", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> queryParameters = new HashMap<>();
    
    // Kafka Configuration
    @NotBlank(message = "Kafka topic is required")
    @Column(name = "kafka_topic")
    private String kafkaTopic;
    
    @Column(name = "kafka_key_field")
    private String kafkaKeyField; // Which DB field to use as Kafka key
    
    @Column(name = "kafka_headers", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, String> kafkaHeaders = new HashMap<>();
    
    // Processing Configuration
    @Column(name = "batch_size")
    private Integer batchSize = 1000; // Process in batches
    
    @Column(name = "max_records")
    private Integer maxRecords; // Limit total records per execution
    
    @Column(name = "offset_field")
    private String offsetField; // Field to track progress (e.g., id, updated_at)
    
    @Column(name = "last_processed_value")
    private String lastProcessedValue; // Track where we left off
    
    // Data Transformation
    @Column(name = "field_mappings", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, String> fieldMappings = new HashMap<>(); // DB field -> Kafka field mapping
    
    @Column(name = "exclude_fields", columnDefinition = "json")
    @Type(JsonType.class)
    private List<String> excludeFields; // Fields to exclude from Kafka message
    
    @Column(name = "include_metadata")
    private Boolean includeMetadata = true;
    
    public Boolean isIncludeMetadata() {
        return includeMetadata != null ? includeMetadata : true;
    } // Include job metadata in message
    
    // Error Handling
    @Column(name = "dead_letter_topic")
    private String deadLetterTopic; // Topic for failed messages
    
    @Column(name = "skip_on_error")
    private Boolean skipOnError = false;
    
    public Boolean isSkipOnError() {
        return skipOnError != null ? skipOnError : false;
    } // Continue processing on individual record errors
    
    @Column(name = "max_retries")
    private Integer maxRetries = 3;
    
    // Connection Settings
    @Column(name = "connection_timeout_seconds")
    private Integer connectionTimeoutSeconds = 30;
    
    @Column(name = "query_timeout_seconds")
    private Integer queryTimeoutSeconds = 300; // 5 minutes for large queries
    
    @Override
    public void validate(JobType type) {
        if (type != JobType.DB_TO_KAFKA) {
            throw new IllegalArgumentException("Invalid job type for DbToKafkaJobPayload: " + type);
        }
        
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("Database URL is required for DB_TO_KAFKA jobs");
        }
        
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("SQL query is required for DB_TO_KAFKA jobs");
        }
        
        if (kafkaTopic == null || kafkaTopic.isBlank()) {
            throw new IllegalArgumentException("Kafka topic is required for DB_TO_KAFKA jobs");
        }
        
        if (batchSize != null && batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        
        if (maxRecords != null && maxRecords <= 0) {
            throw new IllegalArgumentException("Max records must be positive");
        }
        
        if (maxRetries != null && maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        
        if (connectionTimeoutSeconds != null && connectionTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        
        if (queryTimeoutSeconds != null && queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Query timeout must be positive");
        }
        
        // Validate SQL query doesn't contain dangerous operations
        String upperQuery = query.toUpperCase().trim();
        if (upperQuery.contains("DELETE") || upperQuery.contains("UPDATE") || 
            upperQuery.contains("INSERT") || upperQuery.contains("DROP") || 
            upperQuery.contains("ALTER") || upperQuery.contains("CREATE")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed for DB_TO_KAFKA jobs");
        }
    }
}
