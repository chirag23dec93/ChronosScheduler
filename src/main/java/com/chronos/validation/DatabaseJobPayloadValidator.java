package com.chronos.validation;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import com.chronos.domain.model.payload.DatabaseJobPayload;
import org.springframework.stereotype.Component;

@Component
public class DatabaseJobPayloadValidator implements JobPayloadValidator {
    @Override
    public void validate(JobPayload payload) {
        if (!(payload instanceof DatabaseJobPayload dbPayload)) {
            throw new IllegalArgumentException("Invalid payload type for DATABASE job");
        }

        if (dbPayload.getQuery() == null || dbPayload.getQuery().isBlank()) {
            throw new IllegalArgumentException("Query is required for DATABASE jobs");
        }

        if (dbPayload.getDatabaseUrl() == null || dbPayload.getDatabaseUrl().isBlank()) {
            throw new IllegalArgumentException("Database URL is required for DATABASE jobs");
        }

        if (dbPayload.getParameters() == null) {
            throw new IllegalArgumentException("Query parameters must not be null for DATABASE jobs");
        }

        // Validate transaction isolation if specified
        if (dbPayload.getTransactionIsolation() != null) {
            try {
                java.sql.Connection.class.getDeclaredField(
                    "TRANSACTION_" + dbPayload.getTransactionIsolation()
                );
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(
                    "Invalid transaction isolation level: " + dbPayload.getTransactionIsolation()
                );
            }
        }

        // Validate timeout
        if (dbPayload.getQueryTimeoutSeconds() != null && dbPayload.getQueryTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Query timeout must be positive");
        }

        // Validate max rows
        if (dbPayload.getMaxRows() != null && dbPayload.getMaxRows() <= 0) {
            throw new IllegalArgumentException("Max rows must be positive");
        }
    }

    @Override
    public boolean supports(JobType type) {
        return type == JobType.DATABASE;
    }
}
