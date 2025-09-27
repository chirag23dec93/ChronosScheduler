package com.chronos.api.dto.notification;

import com.chronos.domain.model.enums.NotificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateNotificationRequest {
    @NotNull(message = "Channel is required")
    private NotificationChannel channel;

    @NotBlank(message = "Target is required")
    @Pattern(regexp = "^(https?://.*|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6})$",
            message = "Target must be either a valid URL or email address")
    private String target;

    @NotBlank(message = "Template code is required")
    private String templateCode;
}
