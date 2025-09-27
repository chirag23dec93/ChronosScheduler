package com.chronos.service;

import com.chronos.api.dto.notification.CreateNotificationRequest;
import com.chronos.api.dto.notification.NotificationResponse;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface NotificationService {
    void notifyJobCreation(Job job);
    void notifyJobCompletion(Job job, JobRun run);
    void notifyJobFailure(Job job, JobRun run, String reason);
    void notifyMaxRetriesExceeded(Job job, JobRun lastRun);
    void sendNotification(Long notificationId, Map<String, Object> context);
    
    // Notification configuration management
    NotificationResponse addNotification(String jobId, CreateNotificationRequest request);
    Page<NotificationResponse> getJobNotifications(String jobId, Pageable pageable);
    void deleteNotification(String jobId, Long notificationId);
}
