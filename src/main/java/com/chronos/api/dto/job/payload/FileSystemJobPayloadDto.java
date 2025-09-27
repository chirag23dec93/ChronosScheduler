package com.chronos.api.dto.job.payload;

import com.chronos.api.dto.job.JobPayloadDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileSystemJobPayloadDto extends JobPayloadDto {
    @NotBlank(message = "File path is required")
    private String path;

    @NotBlank(message = "Operation is required")
    private String operation;

    private String content;
    
    private String targetPath;
    
    private Boolean createDirectories;
    
    private Boolean overwrite;
    
    @NotNull(message = "File parameters must not be null")
    private Map<String, Object> parameters;
}
