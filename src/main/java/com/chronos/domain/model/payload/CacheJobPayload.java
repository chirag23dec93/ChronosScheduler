package com.chronos.domain.model.payload;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@DiscriminatorValue("CACHE")
@JsonTypeName("CACHE")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CacheJobPayload extends JobPayload {
    private static final List<String> VALID_OPERATIONS = List.of(
        "WARM", "INVALIDATE", "SYNC", "STATS"
    );

    @NotBlank(message = "Cache operation is required")
    @Column(name = "operation")
    private String operation;

    @NotBlank(message = "Cache region is required")
    @Column(name = "region")
    private String region;

    @NotNull(message = "Cache keys must not be null")
    @Column(name = "cache_keys", columnDefinition = "json")
    @Type(JsonType.class)
    private List<String> keys;

    @Column(name = "source_region")
    private String sourceRegion;

    @Column(name = "time_to_live_seconds")
    private Integer timeToLiveSeconds;

    @Column(name = "cache_config", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> cacheConfig;

    @Column(name = "skip_if_exists")
    private Boolean skipIfExists;

    @Column(name = "async")
    private Boolean async;

    @Override
    public void validate(JobType type) {
        if (type != JobType.CACHE) {
            throw new IllegalArgumentException("Invalid job type for CacheJobPayload: " + type);
        }

        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("Cache operation is required for CACHE jobs");
        }

        if (!VALID_OPERATIONS.contains(operation.toUpperCase())) {
            throw new IllegalArgumentException("Invalid cache operation: " + operation);
        }

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Cache region is required for CACHE jobs");
        }

        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("At least one cache key is required for CACHE jobs");
        }

        // Operation-specific validation
        switch (operation.toUpperCase()) {
            case "SYNC" -> {
                if (sourceRegion == null || sourceRegion.isBlank()) {
                    throw new IllegalArgumentException("Source region is required for SYNC operations");
                }
                if (sourceRegion.equals(region)) {
                    throw new IllegalArgumentException("Source region must be different from target region");
                }
            }
            case "WARM" -> {
                if (timeToLiveSeconds != null && timeToLiveSeconds <= 0) {
                    throw new IllegalArgumentException("Time to live must be positive");
                }
            }
        }
    }
}
