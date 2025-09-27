package com.chronos.api.dto.job.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO for cache operation job payloads.
 * Supports operations: WARM, INVALIDATE, SYNC, STATS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheJobPayload {
    
    /**
     * The cache operation to perform (WARM, INVALIDATE, SYNC, STATS)
     */
    @NotBlank
    private String operation;

    /**
     * The target cache region/name
     */
    @NotBlank
    private String region;

    /**
     * List of cache keys to operate on.
     * For INVALIDATE: if empty, clears entire region
     * For STATS: ignored
     */
    @NotNull
    private List<String> keys;

    /**
     * For WARM operation: skip if key already exists
     */
    private Boolean skipIfExists;

    /**
     * For WARM operation: TTL in seconds for cached entries
     */
    private Long timeToLiveSeconds;

    /**
     * For SYNC operation: source cache region to sync from
     */
    private String sourceRegion;
}
