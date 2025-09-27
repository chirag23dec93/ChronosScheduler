package com.chronos.monitoring;

import com.chronos.service.QuartzSchedulerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.quartz.SchedulerException;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Endpoint(id = "cluster")
@RequiredArgsConstructor
public class ClusterInfoEndpoint {

    private final QuartzSchedulerService schedulerService;

    @ReadOperation
    public ClusterInfo getClusterInfo() {
        ClusterInfo info = new ClusterInfo();
        
        try {
            info.setInstanceId(schedulerService.getSchedulerInstanceId());
            info.setClustered(schedulerService.isSchedulerClustered());
            info.setTimestamp(Instant.now());
            
            // Add any additional cluster-specific information
            Map<String, Object> details = new HashMap<>();
            details.put("jvmName", System.getProperty("java.vm.name"));
            details.put("jvmVersion", System.getProperty("java.version"));
            details.put("osName", System.getProperty("os.name"));
            details.put("osVersion", System.getProperty("os.version"));
            details.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            details.put("maxMemory", Runtime.getRuntime().maxMemory());
            details.put("freeMemory", Runtime.getRuntime().freeMemory());
            
            info.setDetails(details);
            info.setStatus("UP");
        } catch (SchedulerException e) {
            info.setStatus("DOWN");
            info.setError(e.getMessage());
        }
        
        return info;
    }

    @Data
    public static class ClusterInfo {
        private String instanceId;
        private boolean clustered;
        private String status;
        private String error;
        private Instant timestamp;
        private Map<String, Object> details;
    }
}
