package com.chronos.api.dto.job;

import com.chronos.api.dto.job.payload.HttpJobPayloadDto;
import com.chronos.api.dto.job.payload.ScriptJobPayloadDto;
import com.chronos.api.dto.job.payload.DatabaseJobPayloadDto;
import com.chronos.api.dto.job.payload.CacheJobPayloadDto;
import com.chronos.api.dto.job.payload.FileSystemJobPayloadDto;
import com.chronos.api.dto.job.payload.MessageQueueJobPayloadDto;
import com.chronos.api.dto.job.payload.ReportJobPayloadDto;
import com.chronos.api.dto.job.payload.DbToKafkaJobPayloadDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpJobPayloadDto.class, name = "HTTP"),
    @JsonSubTypes.Type(value = ScriptJobPayloadDto.class, name = "SCRIPT"),
    @JsonSubTypes.Type(value = DatabaseJobPayloadDto.class, name = "DATABASE"),
    @JsonSubTypes.Type(value = CacheJobPayloadDto.class, name = "CACHE"),
    @JsonSubTypes.Type(value = FileSystemJobPayloadDto.class, name = "FILE_SYSTEM"),
    @JsonSubTypes.Type(value = MessageQueueJobPayloadDto.class, name = "MESSAGE_QUEUE"),
    @JsonSubTypes.Type(value = ReportJobPayloadDto.class, name = "REPORT"),
    @JsonSubTypes.Type(value = DbToKafkaJobPayloadDto.class, name = "DB_TO_KAFKA")
})
public abstract class JobPayloadDto {
    private Map<String, Object> metadata;
}
