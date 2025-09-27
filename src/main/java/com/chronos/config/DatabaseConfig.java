package com.chronos.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    @ConfigurationProperties(prefix = "chronos.database.pool")
    public DatabasePoolProperties databasePoolProperties() {
        return new DatabasePoolProperties();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        DatabasePoolProperties props = databasePoolProperties();
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(props.getMaxPoolSize());
        config.setMinimumIdle(props.getMinIdle());
        config.setIdleTimeout(props.getIdleTimeoutMs());
        config.setMaxLifetime(props.getMaxLifetimeMs());
        config.setConnectionTimeout(props.getConnectionTimeoutMs());
        config.setLeakDetectionThreshold(props.getLeakDetectionThresholdMs());
        config.setValidationTimeout(props.getValidationTimeoutMs());
        config.setPoolName("ChronosMainPool");
        
        // Main application database configuration
        config.setJdbcUrl(props.getUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public DatabaseJobDataSourceFactory databaseJobDataSourceFactory(DatabasePoolProperties props) {
        return new DatabaseJobDataSourceFactory(props);
    }

    @Data
    public static class DatabasePoolProperties {
        private String url;
        private String username;
        private String password;
        private int maxPoolSize = 10;
        private int minIdle = 5;
        private long idleTimeoutMs = 300000; // 5 minutes
        private long maxLifetimeMs = 1800000; // 30 minutes
        private long connectionTimeoutMs = 30000; // 30 seconds
        private long validationTimeoutMs = 5000; // 5 seconds
        private long leakDetectionThresholdMs = 60000; // 1 minute

        // Job-specific pool settings
        private int jobMaxPoolSize = 5;
        private int jobMinIdle = 2;
        private long jobMaxLifetimeMs = 300000; // 5 minutes
        private long jobConnectionTimeoutMs = 10000; // 10 seconds
    }
}
