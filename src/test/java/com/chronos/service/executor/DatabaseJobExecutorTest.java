package com.chronos.service.executor;

import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.domain.model.payload.DatabaseJobPayload;
import com.chronos.event.JobLogEvent;
import com.chronos.exception.JobExecutionException;
import com.chronos.config.DatabaseJobDataSourceFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseJobExecutorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DatabaseJobDataSourceFactory dataSourceFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseJobExecutor databaseJobExecutor;

    private Job testJob;
    private JobRun testJobRun;
    private DatabaseJobPayload databasePayload;

    @BeforeEach
    void setUp() {
        // Setup database payload
        databasePayload = new DatabaseJobPayload();
        databasePayload.setQuery("SELECT COUNT(*) as user_count FROM users");
        databasePayload.setDatabaseUrl("jdbc:mysql://localhost:3306/test_db?user=test&password=test");
        databasePayload.setParameters(Map.of("status", "active"));
        databasePayload.setQueryTimeoutSeconds(30);
        databasePayload.setMaxRows(1000);
        databasePayload.setReadOnly(true);

        // Setup test job
        testJob = new Job();
        testJob.setId("job-123");
        testJob.setName("Test Database Job");
        testJob.setType(JobType.DATABASE);
        testJob.setPayload(databasePayload);

        // Setup job run
        testJobRun = new JobRun();
        testJobRun.setId("run-123");
        testJobRun.setJob(testJob);
        testJobRun.setScheduledTime(Instant.now());
        testJobRun.setAttempt(1);

        // Setup mocks
        when(objectMapper.convertValue(any(), eq(DatabaseJobPayload.class)))
            .thenReturn(databasePayload);
        when(dataSourceFactory.createJdbcTemplate(any(DatabaseJobPayload.class))).thenReturn(jdbcTemplate);
    }

    @Test
    void execute_SelectQuery_Success() {
        // Given
        Map<String, Object> expectedResult = Map.of("user_count", 42);
        when(jdbcTemplate.queryForMap(eq("SELECT COUNT(*) as user_count FROM users"), any(Object[].class)))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(dataSourceFactory).createJdbcTemplate(any(DatabaseJobPayload.class));
        verify(jdbcTemplate).queryForMap(eq("SELECT COUNT(*) as user_count FROM users"), any(Object[].class));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_SelectQueryNoParameters_Success() {
        // Given
        databasePayload.setParameters(Map.of());
        Map<String, Object> expectedResult = Map.of("count", 10);
        when(jdbcTemplate.queryForMap("SELECT COUNT(*) as user_count FROM users"))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).queryForMap("SELECT COUNT(*) as user_count FROM users");
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_UpdateQuery_Success() {
        // Given
        databasePayload.setQuery("UPDATE users SET status = ? WHERE id = ?");
        databasePayload.setReadOnly(false);
        when(jdbcTemplate.update(eq("UPDATE users SET status = ? WHERE id = ?"), any(Object[].class)))
            .thenReturn(5);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).update(eq("UPDATE users SET status = ? WHERE id = ?"), any(Object[].class));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_UpdateQueryNoParameters_Success() {
        // Given
        databasePayload.setQuery("UPDATE users SET last_login = NOW()");
        databasePayload.setParameters(Map.of());
        databasePayload.setReadOnly(false);
        when(jdbcTemplate.update("UPDATE users SET last_login = NOW()"))
            .thenReturn(3);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).update("UPDATE users SET last_login = NOW()");
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_DatabaseConnectionError_ThrowsException() {
        // Given
        when(dataSourceFactory.createJdbcTemplate(any(DatabaseJobPayload.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> databaseJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("Database job execution failed"));
        verify(eventPublisher, atLeast(1)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_SqlException_ThrowsException() {
        // Given
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenThrow(new DataAccessException("SQL syntax error") {});

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> databaseJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("Database job execution failed"));
        verify(eventPublisher, atLeast(1)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_InvalidQuery_ThrowsException() {
        // Given
        databasePayload.setQuery("INVALID SQL QUERY");
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenThrow(new DataAccessException("Invalid SQL") {});

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> databaseJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("Database job execution failed"));
    }

    @Test
    void execute_NullParameters_HandledGracefully() {
        // Given
        databasePayload.setParameters(null);
        Map<String, Object> expectedResult = Map.of("result", "success");
        when(jdbcTemplate.queryForMap("SELECT COUNT(*) as user_count FROM users"))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).queryForMap("SELECT COUNT(*) as user_count FROM users");
    }

    @Test
    void execute_EmptyResult_HandledGracefully() {
        // Given
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenReturn(Map.of());

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).queryForMap(anyString(), any(Object[].class));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ComplexParameterTypes_Success() {
        // Given
        Map<String, Object> complexParameters = Map.of(
            "stringParam", "test",
            "intParam", 123,
            "boolParam", true,
            "nullParam", null
        );
        databasePayload.setParameters(complexParameters);
        
        Map<String, Object> expectedResult = Map.of("processed", 4);
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).queryForMap(anyString(), any(Object[].class));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ReadOnlyFlag_RespectedForSelect() {
        // Given
        databasePayload.setReadOnly(true);
        Map<String, Object> expectedResult = Map.of("data", "value");
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).queryForMap(anyString(), any(Object[].class));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void execute_ReadWriteFlag_AllowsUpdate() {
        // Given
        databasePayload.setQuery("UPDATE users SET status = ?");
        databasePayload.setReadOnly(false);
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
            .thenReturn(1);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
        verify(jdbcTemplate, never()).queryForMap(anyString(), any(Object[].class));
    }

    @Test
    void execute_DataSourceCreation_CalledOnce() {
        // Given
        Map<String, Object> expectedResult = Map.of("result", "ok");
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(dataSourceFactory, times(1)).createJdbcTemplate(any(DatabaseJobPayload.class));
    }

    @Test
    void execute_LoggingEvents_PublishedCorrectly() {
        // Given
        Map<String, Object> expectedResult = Map.of("count", 5);
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
            .thenReturn(expectedResult);

        // When
        assertDoesNotThrow(() -> databaseJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Executing database query");
        }));
        
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Query executed successfully");
        }));
    }
}
