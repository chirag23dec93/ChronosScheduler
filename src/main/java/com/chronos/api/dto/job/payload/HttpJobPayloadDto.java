package com.chronos.api.dto.job.payload;

import com.chronos.api.dto.job.JobPayloadDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpJobPayloadDto extends JobPayloadDto {
    @NotBlank(message = "HTTP URL is required")
    @Pattern(regexp = "^https?://.*", message = "HTTP URL must start with http:// or https://")
    private String httpUrl;

    @NotBlank(message = "HTTP method is required")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)$", message = "Invalid HTTP method")
    private String httpMethod;

    private Map<String, String> httpHeaders;
    private String httpBody;
}
