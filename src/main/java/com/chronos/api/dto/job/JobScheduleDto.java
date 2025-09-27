package com.chronos.api.dto.job;

import com.chronos.domain.model.enums.MisfirePolicy;
import com.chronos.domain.model.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.chronos.validation.CronExpressionValidator;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobScheduleDto {
    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    private Instant runAt;

    @CronExpressionValidator(message = "Invalid cron expression. Format: Seconds Minutes Hours DayOfMonth Month DayOfWeek")
    private String cronExpression;

    private Long intervalSeconds;

    private String timezone;

    @NotNull(message = "Misfire policy is required")
    private MisfirePolicy misfirePolicy;
}
