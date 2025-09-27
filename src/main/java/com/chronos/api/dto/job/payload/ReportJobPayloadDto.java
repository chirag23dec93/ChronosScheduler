package com.chronos.api.dto.job.payload;

import com.chronos.api.dto.job.JobPayloadDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReportJobPayloadDto extends JobPayloadDto {
    @NotBlank(message = "Report type is required")
    private String reportType;

    @NotNull(message = "Report parameters must not be null")
    private Map<String, Object> parameters;

    private List<String> recipients;
    
    private String format;
    
    private Boolean compress;
    
    private String templateName;
    
    private Map<String, Object> templateData;
}
