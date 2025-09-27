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
public class CacheJobPayloadDto extends JobPayloadDto {
    @NotBlank(message = "Cache operation is required")
    private String operation;

    @NotBlank(message = "Cache region is required")
    private String region;

    @NotNull(message = "Cache keys must not be null")
    private List<String> keys;

    private String sourceRegion;
    private Integer timeToLiveSeconds;
    private Map<String, Object> cacheConfig;
    private Boolean skipIfExists;
    private Boolean async;
}
