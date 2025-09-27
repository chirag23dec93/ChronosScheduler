package com.chronos.config;

import com.chronos.domain.model.payload.DatabaseJobPayload;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class DatabaseJobDataSourceFactory {
    private final DatabaseConfig.DatabasePoolProperties poolProperties;
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    public JdbcTemplate createJdbcTemplate(DatabaseJobPayload payload) {
        DataSource dataSource = getOrCreateDataSource(payload);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Configure template based on payload settings
        if (payload.getQueryTimeoutSeconds() != null) {
            jdbcTemplate.setQueryTimeout(payload.getQueryTimeoutSeconds());
        }
        if (payload.getMaxRows() != null) {
            jdbcTemplate.setMaxRows(payload.getMaxRows());
        }

        return jdbcTemplate;
    }

    private DataSource getOrCreateDataSource(DatabaseJobPayload payload) {
        return dataSources.computeIfAbsent(payload.getDatabaseUrl(), url -> {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            
            // Apply credentials if provided in parameters
            Map<String, Object> params = payload.getParameters();
            if (params != null) {
                if (params.containsKey("username")) {
                    config.setUsername(params.get("username").toString());
                }
                if (params.containsKey("password")) {
                    config.setPassword(params.get("password").toString());
                }
            }

            // Job-specific pool settings
            config.setMaximumPoolSize(poolProperties.getJobMaxPoolSize());
            config.setMinimumIdle(poolProperties.getJobMinIdle());
            config.setMaxLifetime(poolProperties.getJobMaxLifetimeMs());
            config.setConnectionTimeout(poolProperties.getJobConnectionTimeoutMs());
            config.setPoolName("ChronosJobPool-" + url.hashCode());

            // Transaction isolation level
            if (payload.getTransactionIsolation() != null) {
                try {
                    config.setTransactionIsolation("TRANSACTION_" + payload.getTransactionIsolation());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid transaction isolation level: {}", payload.getTransactionIsolation());
                }
            }

            return new HikariDataSource(config);
        });
    }

    public void cleanup() {
        dataSources.values().forEach(ds -> {
            try {
                ds.close();
            } catch (Exception e) {
                log.error("Error closing data source", e);
            }
        });
        dataSources.clear();
    }
}
