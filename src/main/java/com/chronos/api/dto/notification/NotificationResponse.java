package com.chronos.api.dto.notification;

import com.chronos.domain.model.enums.NotificationChannel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private Long id;
    private String jobId;
    private String userEmail;
    private NotificationChannel channel;
    private String target;
    private String templateCode;
    private Instant sentAt;
    private Map<String, Object> payload;
}
