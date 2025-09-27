package com.chronos.domain.model;

import com.chronos.domain.model.payload.*;
import com.chronos.domain.model.enums.JobType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "job_payloads")
@Getter
@Setter
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payload_type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpJobPayload.class, name = "HTTP"),
    @JsonSubTypes.Type(value = ScriptJobPayload.class, name = "SCRIPT"),
    @JsonSubTypes.Type(value = DatabaseJobPayload.class, name = "DATABASE"),
    @JsonSubTypes.Type(value = FileSystemJobPayload.class, name = "FILE_SYSTEM"),
    @JsonSubTypes.Type(value = MessageQueueJobPayload.class, name = "MESSAGE_QUEUE"),
    @JsonSubTypes.Type(value = CacheJobPayload.class, name = "CACHE"),
    @JsonSubTypes.Type(value = ReportJobPayload.class, name = "REPORT"),
    @JsonSubTypes.Type(value = DbToKafkaJobPayload.class, name = "DB_TO_KAFKA")
})
public abstract class JobPayload {
    @Id
    @Column(name = "job_id")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "job_id")
    @JsonBackReference("job-payload")
    private Job job;

    @Column(name = "metadata", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> metadata;



    /**
     * Validate this payload for its specific job type
     * @param type The job type to validate against
     */
    public abstract void validate(JobType type);
}
