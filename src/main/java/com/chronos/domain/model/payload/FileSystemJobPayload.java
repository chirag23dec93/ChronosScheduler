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
@DiscriminatorValue("FILE_SYSTEM")
@JsonTypeName("FILE_SYSTEM")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class FileSystemJobPayload extends JobPayload {
    private static final List<String> VALID_OPERATIONS = List.of(
        "READ", "WRITE", "DELETE", "COPY", "MOVE", "LIST", "WATCH"
    );

    @NotBlank(message = "File path is required")
    @Column(name = "path")
    private String path;

    @NotBlank(message = "Operation is required")
    @Column(name = "operation")
    private String operation;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "target_path")
    private String targetPath;

    @Column(name = "create_directories")
    private Boolean createDirectories;

    @Column(name = "overwrite")
    private Boolean overwrite;

    @Column(name = "parameters", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> parameters;

    @Override
    public void validate(JobType type) {
        if (type != JobType.FILE_SYSTEM) {
            throw new IllegalArgumentException("Invalid job type for FileSystemJobPayload: " + type);
        }

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("File path is required for FILE_SYSTEM jobs");
        }

        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("Operation is required for FILE_SYSTEM jobs");
        }

        if (!VALID_OPERATIONS.contains(operation.toUpperCase())) {
            throw new IllegalArgumentException("Invalid file system operation: " + operation);
        }

        // Operation-specific validation
        switch (operation.toUpperCase()) {
            case "WRITE" -> {
                if (content == null) {
                    throw new IllegalArgumentException("Content is required for WRITE operations");
                }
            }
            case "COPY", "MOVE" -> {
                if (targetPath == null || targetPath.isBlank()) {
                    throw new IllegalArgumentException("Target path is required for " + operation + " operations");
                }
                if (targetPath.equals(path)) {
                    throw new IllegalArgumentException("Target path must be different from source path");
                }
            }
        }
    }
}
