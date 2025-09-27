package com.chronos.service.executor;

import com.chronos.api.dto.job.payload.CacheJobPayload;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.exception.JobExecutionException;
import org.springframework.context.ApplicationContext;
import com.chronos.service.JobExecutorService;
import com.chronos.service.cache.CacheStatisticsCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheJobExecutor {
    
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheStatisticsCollector statisticsCollector;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Timed(value = "cache.job.execution", extraTags = {"operation", "#{payload.operation}"})
    public void execute(Job job, JobRun run) {
        try {
            CacheJobPayload payload = objectMapper.convertValue(
                job.getPayload(), CacheJobPayload.class);

            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Executing cache operation %s on region %s", 
                    payload.getOperation(), payload.getRegion()));

            switch (payload.getOperation().toUpperCase()) {
                case "WARM":
                    warmCache(payload, run);
                    break;
                case "INVALIDATE":
                    invalidateCache(payload, run);
                    break;
                case "SYNC":
                    syncCache(payload, run);
                    break;
                case "STATS":
                    collectStats(payload, run);
                    break;
                default:
                    throw new JobExecutionException("Unsupported operation: " + 
                        payload.getOperation());
            }

            getJobExecutorService().logOutput(run, "INFO", "Cache operation completed successfully");

        } catch (Exception e) {
            String error = String.format("Cache job execution failed: %s", e.getMessage());
            getJobExecutorService().logOutput(run, "ERROR", error);
            throw new JobExecutionException(error, e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void warmCache(CacheJobPayload payload, JobRun run) {
        var cache = cacheManager.getCache(payload.getRegion());
        if (cache == null) {
            throw new JobExecutionException("Cache region not found: " + payload.getRegion());
        }

        for (String key : payload.getKeys()) {
            if (payload.getSkipIfExists() && cache.get(key) != null) {
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Skipping existing key: %s", key));
                continue;
            }

            // In a real implementation, this would fetch data from the source
            Object value = fetchDataForKey(key);
            cache.put(key, value);

            if (payload.getTimeToLiveSeconds() != null) {
                redisTemplate.expire(key, payload.getTimeToLiveSeconds(), TimeUnit.SECONDS);
            }

            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Warmed cache for key: %s", key));
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void invalidateCache(CacheJobPayload payload, JobRun run) {
        var cache = cacheManager.getCache(payload.getRegion());
        if (cache == null) {
            throw new JobExecutionException("Cache region not found: " + payload.getRegion());
        }

        if (payload.getKeys().isEmpty()) {
            cache.clear();
            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Cleared entire cache region: %s", payload.getRegion()));
        } else {
            for (String key : payload.getKeys()) {
                cache.evict(key);
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Invalidated cache key: %s", key));
            }
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void syncCache(CacheJobPayload payload, JobRun run) {
        if (payload.getSourceRegion() == null) {
            throw new JobExecutionException("Source region is required for sync operation");
        }

        var sourceCache = cacheManager.getCache(payload.getSourceRegion());
        var targetCache = cacheManager.getCache(payload.getRegion());

        if (sourceCache == null || targetCache == null) {
            throw new JobExecutionException("Cache region not found");
        }

        for (String key : payload.getKeys()) {
            var value = sourceCache.get(key);
            if (value != null) {
                targetCache.put(key, value.get());
                getJobExecutorService().logOutput(run, "INFO", 
                    String.format("Synced key %s from %s to %s", 
                        key, payload.getSourceRegion(), payload.getRegion()));
            }
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void collectStats(CacheJobPayload payload, JobRun run) {
        var cache = cacheManager.getCache(payload.getRegion());
        if (cache == null) {
            throw new JobExecutionException("Cache region not found: " + payload.getRegion());
        }

        Map<String, Object> stats = statisticsCollector.collectStatistics(payload.getRegion());

        getJobExecutorService().logOutput(run, "INFO", "Cache statistics collected", stats);
    }

    private Object fetchDataForKey(String key) {
        // Simulate data fetching with random delay to test retry mechanism
        try {
            if (Math.random() < 0.3) { // 30% chance of failure
                throw new RuntimeException("Simulated fetch failure");
            }
            Thread.sleep((long) (Math.random() * 1000)); // Random delay up to 1 second
            return "cached_value_" + key + "_" + System.currentTimeMillis();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Data fetch interrupted", e);
        }
    }

    private JobExecutorService getJobExecutorService() {
        return applicationContext.getBean(JobExecutorService.class);
    }
}
