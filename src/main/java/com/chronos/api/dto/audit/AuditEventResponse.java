package com.chronos.api.dto.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEventResponse {
    private Long id;
    private String userEmail;
    private String action;
    private String entityType;
    private String entityId;
    private Instant createdAt;
    private Map<String, Object> details;
}
