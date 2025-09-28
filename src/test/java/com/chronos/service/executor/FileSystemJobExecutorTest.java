package com.chronos.service.executor;

import com.chronos.domain.model.*;
import com.chronos.domain.model.enums.*;
import com.chronos.domain.model.payload.FileSystemJobPayload;
import com.chronos.event.JobLogEvent;
import com.chronos.exception.JobExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileSystemJobExecutorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FileSystemJobExecutor fileSystemJobExecutor;

    @TempDir
    Path tempDir;

    private Job testJob;
    private JobRun testJobRun;
    private FileSystemJobPayload fileSystemPayload;

    @BeforeEach
    void setUp() {
        // Setup file system payload
        fileSystemPayload = new FileSystemJobPayload();
        fileSystemPayload.setOperation("READ");
        fileSystemPayload.setPath(tempDir.resolve("test.txt").toString());
        fileSystemPayload.setParameters(Map.of());

        // Setup test job
        testJob = new Job();
        testJob.setId("job-123");
        testJob.setName("Test File System Job");
        testJob.setType(JobType.FILE_SYSTEM);
        testJob.setPayload(fileSystemPayload);

        // Setup job run
        testJobRun = new JobRun();
        testJobRun.setId("run-123");
        testJobRun.setJob(testJob);
        testJobRun.setScheduledTime(Instant.now());
        testJobRun.setAttempt(1);

        // Setup mocks
        when(objectMapper.convertValue(any(), eq(FileSystemJobPayload.class)))
            .thenReturn(fileSystemPayload);
    }

    @Test
    void execute_ReadOperation_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");
        fileSystemPayload.setPath(testFile.toString());

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("File system operation completed successfully");
        }));
    }

    @Test
    void execute_WriteOperation_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("write-test.txt");
        fileSystemPayload.setOperation("WRITE");
        fileSystemPayload.setPath(testFile.toString());
        fileSystemPayload.setContent("Test content for writing");

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertTrue(Files.exists(testFile));
        assertEquals("Test content for writing", Files.readString(testFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_CopyOperation_Success() throws IOException {
        // Given
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        Files.writeString(sourceFile, "Content to copy");
        
        fileSystemPayload.setOperation("COPY");
        fileSystemPayload.setPath(sourceFile.toString());
        fileSystemPayload.setTargetPath(targetFile.toString());

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertTrue(Files.exists(sourceFile)); // Original should still exist
        assertTrue(Files.exists(targetFile)); // Copy should exist
        assertEquals("Content to copy", Files.readString(targetFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_MoveOperation_Success() throws IOException {
        // Given
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        Files.writeString(sourceFile, "Content to move");
        
        fileSystemPayload.setOperation("MOVE");
        fileSystemPayload.setPath(sourceFile.toString());
        fileSystemPayload.setTargetPath(targetFile.toString());

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertFalse(Files.exists(sourceFile)); // Original should be gone
        assertTrue(Files.exists(targetFile)); // Target should exist
        assertEquals("Content to move", Files.readString(targetFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_DeleteOperation_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("delete-me.txt");
        Files.writeString(testFile, "This file will be deleted");
        
        fileSystemPayload.setOperation("DELETE");
        fileSystemPayload.setPath(testFile.toString());

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertFalse(Files.exists(testFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ListOperation_Success() throws IOException {
        // Given
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));
        
        fileSystemPayload.setOperation("LIST");
        fileSystemPayload.setPath(tempDir.toString());

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Listed") && 
                   logEvent.getMessage().contains("items");
        }));
    }

    @Test
    void execute_InvalidOperation_ThrowsException() {
        // Given
        fileSystemPayload.setOperation("INVALID_OPERATION");

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> fileSystemJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("File system job execution failed"));
        verify(eventPublisher, atLeast(1)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_ReadNonExistentFile_ThrowsException() {
        // Given
        fileSystemPayload.setOperation("READ");
        fileSystemPayload.setPath("/nonexistent/file.txt");

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> fileSystemJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("File system job execution failed"));
        verify(eventPublisher, atLeast(1)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_WriteWithoutContent_ThrowsException() {
        // Given
        fileSystemPayload.setOperation("WRITE");
        fileSystemPayload.setPath(tempDir.resolve("test.txt").toString());
        fileSystemPayload.setContent(null); // No content provided

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> fileSystemJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("File system job execution failed"));
    }

    @Test
    void execute_CopyWithoutTargetPath_ThrowsException() {
        // Given
        fileSystemPayload.setOperation("COPY");
        fileSystemPayload.setPath(tempDir.resolve("source.txt").toString());
        fileSystemPayload.setTargetPath(null); // No target path

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> fileSystemJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("File system job execution failed"));
    }

    @Test
    void execute_WriteWithCreateDirectories_Success() throws IOException {
        // Given
        Path nestedFile = tempDir.resolve("nested/dir/test.txt");
        fileSystemPayload.setOperation("WRITE");
        fileSystemPayload.setPath(nestedFile.toString());
        fileSystemPayload.setContent("Nested content");
        fileSystemPayload.setCreateDirectories(true);

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertTrue(Files.exists(nestedFile));
        assertTrue(Files.exists(nestedFile.getParent()));
        assertEquals("Nested content", Files.readString(nestedFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_WriteWithOverwrite_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("overwrite-test.txt");
        Files.writeString(testFile, "Original content");
        
        fileSystemPayload.setOperation("WRITE");
        fileSystemPayload.setPath(testFile.toString());
        fileSystemPayload.setContent("New content");
        fileSystemPayload.setOverwrite(true);

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertEquals("New content", Files.readString(testFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_WriteWithoutOverwrite_ThrowsException() throws IOException {
        // Given
        Path testFile = tempDir.resolve("existing-file.txt");
        Files.writeString(testFile, "Existing content");
        
        fileSystemPayload.setOperation("WRITE");
        fileSystemPayload.setPath(testFile.toString());
        fileSystemPayload.setContent("New content");
        fileSystemPayload.setOverwrite(false);

        // When & Then
        JobExecutionException exception = assertThrows(
            JobExecutionException.class,
            () -> fileSystemJobExecutor.execute(testJob, testJobRun)
        );

        assertTrue(exception.getMessage().contains("File system job execution failed"));
        // Original content should remain unchanged
        assertEquals("Existing content", Files.readString(testFile));
    }

    @Test
    void execute_CopyToExistingFileWithOverwrite_Success() throws IOException {
        // Given
        Path sourceFile = tempDir.resolve("source.txt");
        Path targetFile = tempDir.resolve("target.txt");
        Files.writeString(sourceFile, "Source content");
        Files.writeString(targetFile, "Target content");
        
        fileSystemPayload.setOperation("COPY");
        fileSystemPayload.setPath(sourceFile.toString());
        fileSystemPayload.setTargetPath(targetFile.toString());
        fileSystemPayload.setOverwrite(true);

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        assertEquals("Source content", Files.readString(targetFile));
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
    }

    @Test
    void execute_LoggingEvents_PublishedCorrectly() throws IOException {
        // Given
        Path testFile = tempDir.resolve("log-test.txt");
        Files.writeString(testFile, "Test content");
        fileSystemPayload.setPath(testFile.toString());

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("Executing file system operation READ");
        }));
        
        verify(eventPublisher, times(1)).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("File system operation completed successfully");
        }));
    }

    @Test
    void execute_WithParameters_Success() throws IOException {
        // Given
        Path testFile = tempDir.resolve("param-test.txt");
        Files.writeString(testFile, "Parameter test content");
        
        fileSystemPayload.setPath(testFile.toString());
        fileSystemPayload.setParameters(Map.of(
            "encoding", "UTF-8",
            "bufferSize", 8192,
            "createBackup", true
        ));

        // When
        assertDoesNotThrow(() -> fileSystemJobExecutor.execute(testJob, testJobRun));

        // Then
        verify(eventPublisher, atLeast(2)).publishEvent(any(JobLogEvent.class));
        verify(eventPublisher).publishEvent(argThat(event -> {
            JobLogEvent logEvent = (JobLogEvent) event;
            return logEvent.getMessage().contains("parameters");
        }));
    }
}
