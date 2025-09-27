package com.chronos.service.executor;

import com.chronos.domain.model.payload.FileSystemJobPayload;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.exception.JobExecutionException;
import com.chronos.service.JobExecutorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemJobExecutor {
    
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute(Job job, JobRun run) {
        try {
            FileSystemJobPayload payload = objectMapper.convertValue(
                job.getPayload(), FileSystemJobPayload.class);

            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Executing file system operation %s on %s", 
                    payload.getOperation(), payload.getPath()));

            List<Path> matchingFiles = findMatchingFiles(payload);
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Found %d matching files", matchingFiles.size()));

            switch (payload.getOperation().toUpperCase()) {
                case "PROCESS":
                    processFiles(matchingFiles, run);
                    break;
                case "MOVE":
                    moveFiles(matchingFiles, payload.getTargetPath(), run);
                    break;
                case "COPY":
                    copyFiles(matchingFiles, payload.getTargetPath(), run);
                    break;
                case "DELETE":
                    deleteFiles(matchingFiles, run);
                    break;
                case "COMPRESS":
                    compressFiles(matchingFiles, payload.getTargetPath(), 
                        payload.getParameters().get("compressionType").toString(), run);
                    break;
                default:
                    throw new JobExecutionException("Unsupported operation type: " + 
                        payload.getOperation());
            }

            getJobExecutorService().logOutput(run, "INFO", "File system operation completed successfully");

        } catch (Exception e) {
            String error = String.format("File system job execution failed: %s", e.getMessage());
            getJobExecutorService().logOutput(run, "ERROR", error);
            throw new JobExecutionException(error, e);
        }
    }

    private List<Path> findMatchingFiles(FileSystemJobPayload payload) throws IOException {
        List<String> patterns = payload.getParameters().containsKey("filePatterns") ?
            (List<String>)payload.getParameters().get("filePatterns") :
            List.of("*");

        List<PathMatcher> matchers = patterns.stream()
            .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
            .collect(Collectors.toList());
        
        try (var paths = Files.walk(Path.of(payload.getPath()), 
                payload.getParameters().containsKey("recursive") && 
                (Boolean)payload.getParameters().get("recursive") ? Integer.MAX_VALUE : 1)) {
            return paths.filter(Files::isRegularFile)
                       .filter(path -> matchers.stream().anyMatch(matcher -> matcher.matches(path.getFileName())))
                       .collect(Collectors.toList());
        }
    }

    private void processFiles(List<Path> files, JobRun run) {
        // Implementation depends on specific processing requirements
        getJobExecutorService().logOutput(run, "INFO", "Processing files...");
    }

    private void moveFiles(List<Path> files, String destinationPath, JobRun run) throws IOException {
        Path destDir = Path.of(destinationPath);
        Files.createDirectories(destDir);
        
        for (Path file : files) {
            Path target = destDir.resolve(file.getFileName());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Moved %s to %s", file, target));
        }
    }

    private void copyFiles(List<Path> files, String destinationPath, JobRun run) throws IOException {
        Path destDir = Path.of(destinationPath);
        Files.createDirectories(destDir);
        
        for (Path file : files) {
            Path target = destDir.resolve(file.getFileName());
            Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Copied %s to %s", file, target));
        }
    }

    private void deleteFiles(List<Path> files, JobRun run) throws IOException {
        for (Path file : files) {
            Files.delete(file);
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Deleted %s", file));
        }
    }

    private void compressFiles(List<Path> files, String destinationPath, 
            String compressionType, JobRun run) throws IOException {
        Path destFile = Path.of(destinationPath);
        Files.createDirectories(destFile.getParent());
        
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(destFile))) {
            for (Path file : files) {
                ZipEntry entry = new ZipEntry(file.getFileName().toString());
                zos.putNextEntry(entry);
                Files.copy(file, zos);
                zos.closeEntry();
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Added %s to archive", file));
            }
        }
    }

    private JobExecutorService getJobExecutorService() {
        return applicationContext.getBean(JobExecutorService.class);
    }
}
