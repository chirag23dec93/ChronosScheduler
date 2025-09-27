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

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@DiscriminatorValue("DATABASE")
@JsonTypeName("DATABASE")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DatabaseJobPayload extends JobPayload {
    private static final List<String> VALID_ISOLATION_LEVELS = List.of(
        "READ_UNCOMMITTED",
        "READ_COMMITTED",
        "REPEATABLE_READ",
        "SERIALIZABLE"
    );
    
    @NotBlank(message = "SQL query is required")
    @Column(name = "sql_query", columnDefinition = "text")
    private String query;

    @NotBlank(message = "Database URL is required")
    @Column(name = "database_url")
    private String databaseUrl;

    @Column(name = "query_parameters", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> parameters;

    @Column(name = "transaction_isolation")
    private String transactionIsolation;

    @Column(name = "query_timeout_seconds")
    private Integer queryTimeoutSeconds;

    @Column(name = "max_rows")
    private Integer maxRows;

    @Column(name = "read_only")
    private Boolean readOnly;

    @Override
    public void validate(JobType type) {
        if (type != JobType.DATABASE) {
            throw new IllegalArgumentException("Invalid job type for DatabaseJobPayload: " + type);
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("SQL query is required for database jobs");
        }
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("Database URL is required for database jobs");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Query parameters must not be null");
        }
        if (queryTimeoutSeconds != null && queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Query timeout must be positive");
        }
        if (maxRows != null && maxRows <= 0) {
            throw new IllegalArgumentException("Max rows must be positive");
        }
        if (transactionIsolation != null && !VALID_ISOLATION_LEVELS.contains(transactionIsolation.toUpperCase())) {
            throw new IllegalArgumentException("Invalid transaction isolation level: " + transactionIsolation + 
                ". Valid levels are: " + String.join(", ", VALID_ISOLATION_LEVELS));
        }
    }
}
