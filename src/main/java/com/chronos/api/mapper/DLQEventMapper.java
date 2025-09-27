package com.chronos.api.mapper;

import com.chronos.api.dto.dlq.DLQEventResponse;
import com.chronos.domain.model.DLQEvent;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DLQEventMapper {
    
    @Mapping(target = "jobId", source = "job.id")
    @Mapping(target = "jobName", source = "job.name")
    @Mapping(target = "lastRunId", source = "lastRun.id")
    @Mapping(target = "resolved", expression = "java(isResolved(dlqEvent))")
    DLQEventResponse toDLQEventResponse(DLQEvent dlqEvent);
    
    @AfterMapping
    default boolean isResolved(DLQEvent dlqEvent) {
        return dlqEvent.getJob().getStatus() != com.chronos.domain.model.enums.JobStatus.FAILED;
    }
}
