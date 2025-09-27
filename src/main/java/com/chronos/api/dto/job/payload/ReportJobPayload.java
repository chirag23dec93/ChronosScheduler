package com.chronos.api.dto.job.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportJobPayload {
    
    @NotBlank(message = "Report type is required")
    @Pattern(regexp = "^(PDF|EXCEL|CSV|HTML)$", message = "Invalid report type. Must be one of: PDF, EXCEL, CSV, HTML")
    private String reportType;
    
    @NotBlank(message = "Template name is required")
    private String templateName;
    
    private Map<String, Object> parameters;
    
    private String timezone;
    
    private Map<String, Object> dateRange;
    
    private String outputPath;
    
    private Boolean compress;
    
    private List<String> recipients;
    
    private Map<String, Object> formatting;
}
