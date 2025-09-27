package com.chronos.api.mapper;

import com.chronos.api.dto.notification.CreateNotificationRequest;
import com.chronos.api.dto.notification.NotificationResponse;
import com.chronos.domain.model.Notification;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {
    
    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "userEmail", source = "user.email")
    NotificationResponse toNotificationResponse(Notification notification);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "payload", ignore = true)
    Notification toNotification(CreateNotificationRequest request);
}
