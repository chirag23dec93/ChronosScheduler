package com.chronos.api.dto.job.payload;

import com.chronos.api.dto.job.JobPayloadDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DbToKafkaJobPayloadDto extends JobPayloadDto {
    
    // Database Configuration
    @NotBlank(message = "Database URL is required")
    private String databaseUrl;
    
    @NotBlank(message = "SQL query is required")
    private String query;
    
    @NotNull(message = "Query parameters must not be null")
    private Map<String, Object> queryParameters = new HashMap<>();
    
    // Kafka Configuration
    @NotBlank(message = "Kafka topic is required")
    private String kafkaTopic;
    
    private String kafkaKeyField;
    
    @NotNull(message = "Kafka headers must not be null")
    private Map<String, String> kafkaHeaders = new HashMap<>();
    
    // Processing Configuration
    private Integer batchSize = 1000;
    
    private Integer maxRecords;
    
    private String offsetField;
    
    private String lastProcessedValue;
    
    // Data Transformation
    @NotNull(message = "Field mappings must not be null")
    private Map<String, String> fieldMappings = new HashMap<>();
    
    private List<String> excludeFields;
    
    private Boolean includeMetadata = true;
    
    // Error Handling
    private String deadLetterTopic;
    
    private Boolean skipOnError = false;
    
    private Integer maxRetries = 3;
    
    // Connection Settings
    private Integer connectionTimeoutSeconds = 30;
    
    private Integer queryTimeoutSeconds = 300;
}
