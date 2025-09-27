package com.chronos.service.executor;

import com.chronos.config.DatabaseJobDataSourceFactory;
import com.chronos.domain.model.payload.DatabaseJobPayload;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.event.JobLogEvent;
import com.chronos.exception.JobExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseJobExecutor implements JobTypeExecutor {
    
    private final ObjectMapper objectMapper;
    private final DatabaseJobDataSourceFactory dataSourceFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Job job, JobRun run) {
        try {
            DatabaseJobPayload payload = objectMapper.convertValue(
                job.getPayload(), DatabaseJobPayload.class);

            eventPublisher.publishEvent(new JobLogEvent(this, run, "INFO",
                String.format("Executing database query on %s", payload.getDatabaseUrl())));

            JdbcTemplate jdbcTemplate = dataSourceFactory.createJdbcTemplate(payload);
            Map<String, Object> result;

            if (payload.getReadOnly() != null && payload.getReadOnly()) {
                // For read-only queries
                if (payload.getParameters() != null && !payload.getParameters().isEmpty()) {
                    try {
                        // Try single row first
                        result = jdbcTemplate.queryForMap(payload.getQuery(), payload.getParameters().values().toArray());
                    } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                        // If multiple rows, return them as a list
                        result = Map.of("rows", jdbcTemplate.queryForList(payload.getQuery(), payload.getParameters().values().toArray()));
                    }
                } else {
                    try {
                        // Try single row first
                        result = jdbcTemplate.queryForMap(payload.getQuery());
                    } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
                        // If multiple rows, return them as a list
                        result = Map.of("rows", jdbcTemplate.queryForList(payload.getQuery()));
                    }
                }
            } else {
                // For write queries, use update and return affected rows
                if (payload.getParameters() != null && !payload.getParameters().isEmpty()) {
                    int rowsAffected = jdbcTemplate.update(payload.getQuery(), payload.getParameters().values().toArray());
                    result = Map.of("rowsAffected", rowsAffected);
                } else {
                    int rowsAffected = jdbcTemplate.update(payload.getQuery());
                    result = Map.of("rowsAffected", rowsAffected);
                }
            }

            eventPublisher.publishEvent(new JobLogEvent(this, run, "INFO", "Query executed successfully", result));

        } catch (Exception e) {
            String error = String.format("Database job execution failed: %s", e.getMessage());
            eventPublisher.publishEvent(new JobLogEvent(this, run, "ERROR", error));
            throw new JobExecutionException(error, e);
        }
    }


}
