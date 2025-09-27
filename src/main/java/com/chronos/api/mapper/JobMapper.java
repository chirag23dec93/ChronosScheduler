package com.chronos.api.mapper;

import com.chronos.api.dto.job.CreateJobRequest;
import com.chronos.api.dto.job.JobResponse;
import com.chronos.api.dto.job.JobScheduleDto;
import com.chronos.api.dto.job.JobPayloadDto;
import com.chronos.api.dto.job.RetryPolicyDto;
import com.chronos.api.dto.job.JobRunSummaryDto;
import com.chronos.api.dto.job.payload.HttpJobPayloadDto;
import com.chronos.api.dto.job.payload.ScriptJobPayloadDto;
import com.chronos.api.dto.job.payload.DatabaseJobPayloadDto;
import com.chronos.api.dto.job.payload.CacheJobPayloadDto;
import com.chronos.api.dto.job.payload.FileSystemJobPayloadDto;
import com.chronos.api.dto.job.payload.MessageQueueJobPayloadDto;
import com.chronos.api.dto.job.payload.ReportJobPayloadDto;
import com.chronos.api.dto.job.payload.DbToKafkaJobPayloadDto;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.JobSchedule;
import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.RetryPolicy;
import com.chronos.domain.model.payload.HttpJobPayload;
import com.chronos.domain.model.payload.ScriptJobPayload;
import com.chronos.domain.model.payload.DatabaseJobPayload;
import com.chronos.domain.model.payload.CacheJobPayload;
import com.chronos.domain.model.payload.FileSystemJobPayload;
import com.chronos.domain.model.payload.MessageQueueJobPayload;
import com.chronos.domain.model.payload.ReportJobPayload;
import com.chronos.domain.model.payload.DbToKafkaJobPayload;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobMapper {
    
    @Mapping(target = "ownerEmail", source = "owner.email")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobResponse toJobResponse(Job job);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Job toJob(CreateJobRequest request);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobScheduleDto toJobScheduleDto(JobSchedule schedule);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobSchedule toJobSchedule(JobScheduleDto dto);
    
    @SubclassMapping(source = HttpJobPayload.class, target = HttpJobPayloadDto.class)
    @SubclassMapping(source = ScriptJobPayload.class, target = ScriptJobPayloadDto.class)
    @SubclassMapping(source = DatabaseJobPayload.class, target = DatabaseJobPayloadDto.class)
    @SubclassMapping(source = CacheJobPayload.class, target = CacheJobPayloadDto.class)
    @SubclassMapping(source = FileSystemJobPayload.class, target = FileSystemJobPayloadDto.class)
    @SubclassMapping(source = MessageQueueJobPayload.class, target = MessageQueueJobPayloadDto.class)
    @SubclassMapping(source = ReportJobPayload.class, target = ReportJobPayloadDto.class)
    @SubclassMapping(source = DbToKafkaJobPayload.class, target = DbToKafkaJobPayloadDto.class)
    @Mapping(target = "metadata", source = "metadata")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobPayloadDto toJobPayloadDto(JobPayload payload);
    
    @SubclassMapping(source = HttpJobPayloadDto.class, target = HttpJobPayload.class)
    @SubclassMapping(source = ScriptJobPayloadDto.class, target = ScriptJobPayload.class)
    @SubclassMapping(source = DatabaseJobPayloadDto.class, target = DatabaseJobPayload.class)
    @SubclassMapping(source = CacheJobPayloadDto.class, target = CacheJobPayload.class)
    @SubclassMapping(source = FileSystemJobPayloadDto.class, target = FileSystemJobPayload.class)
    @SubclassMapping(source = MessageQueueJobPayloadDto.class, target = MessageQueueJobPayload.class)
    @SubclassMapping(source = ReportJobPayloadDto.class, target = ReportJobPayload.class)
    @SubclassMapping(source = DbToKafkaJobPayloadDto.class, target = DbToKafkaJobPayload.class)
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "job", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobPayload toJobPayload(JobPayloadDto dto);

    @Mapping(target = "query", source = "query")
    @Mapping(target = "databaseUrl", source = "databaseUrl")
    @Mapping(target = "parameters", source = "parameters")
    @Mapping(target = "transactionIsolation", source = "transactionIsolation")
    @Mapping(target = "queryTimeoutSeconds", source = "queryTimeoutSeconds")
    @Mapping(target = "maxRows", source = "maxRows")
    @Mapping(target = "readOnly", source = "readOnly")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    DatabaseJobPayload toDatabaseJobPayload(DatabaseJobPayloadDto dto);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RetryPolicyDto toRetryPolicyDto(RetryPolicy policy);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "job", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    RetryPolicy toRetryPolicy(RetryPolicyDto dto);
    
    @Mapping(target = "id", source = "id")
    @Mapping(target = "scheduledTime", source = "scheduledTime")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "attempt", source = "attempt")
    @Mapping(target = "outcome", source = "outcome")
    @Mapping(target = "exitCode", source = "exitCode")
    @Mapping(target = "errorMessage", source = "errorMessage")
    @Mapping(target = "workerId", source = "workerId")
    @Mapping(target = "durationMs", source = "durationMs")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    JobRunSummaryDto toJobRunSummaryDto(JobRun run);
    
    @AfterMapping
    default void validatePayload(@MappingTarget Job job) {
        if (job.getPayload() != null) {
            job.getPayload().validate(job.getType());
        }
    }
    
    @ObjectFactory
    default JobPayloadDto createJobPayloadDto(JobPayload payload) {
        if (payload == null) return null;
        if (payload instanceof HttpJobPayload) return new HttpJobPayloadDto();
        if (payload instanceof ScriptJobPayload) return new ScriptJobPayloadDto();
        if (payload instanceof DatabaseJobPayload) return new DatabaseJobPayloadDto();
        if (payload instanceof CacheJobPayload) return new CacheJobPayloadDto();
        if (payload instanceof FileSystemJobPayload) return new FileSystemJobPayloadDto();
        if (payload instanceof MessageQueueJobPayload) return new MessageQueueJobPayloadDto();
        if (payload instanceof ReportJobPayload) return new ReportJobPayloadDto();
        if (payload instanceof DbToKafkaJobPayload) return new DbToKafkaJobPayloadDto();
        throw new IllegalArgumentException("Unknown payload type: " + payload.getClass());
    }
    
    @ObjectFactory
    default JobPayload createJobPayload(JobPayloadDto dto) {
        if (dto == null) return null;
        if (dto instanceof HttpJobPayloadDto) return new HttpJobPayload();
        if (dto instanceof ScriptJobPayloadDto) return new ScriptJobPayload();
        if (dto instanceof DatabaseJobPayloadDto) return new DatabaseJobPayload();
        if (dto instanceof CacheJobPayloadDto) return new CacheJobPayload();
        if (dto instanceof FileSystemJobPayloadDto) return new FileSystemJobPayload();
        if (dto instanceof MessageQueueJobPayloadDto) return new MessageQueueJobPayload();
        if (dto instanceof ReportJobPayloadDto) return new ReportJobPayload();
        if (dto instanceof DbToKafkaJobPayloadDto) return new DbToKafkaJobPayload();
        throw new IllegalArgumentException("Unknown payload type: " + dto.getClass());
    }
}
