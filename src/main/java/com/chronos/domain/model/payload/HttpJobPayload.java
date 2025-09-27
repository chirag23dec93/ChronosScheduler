package com.chronos.domain.model.payload;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

@Getter
@Setter
@Entity
@DiscriminatorValue("HTTP")
@JsonTypeName("HTTP")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class HttpJobPayload extends JobPayload {
    @NotBlank(message = "HTTP URL is required")
    @Pattern(regexp = "^https?://.*", message = "HTTP URL must start with http:// or https://")
    @Column(name = "http_url")
    private String httpUrl;

    @NotBlank(message = "HTTP method is required")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)$", message = "Invalid HTTP method")
    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "http_body")
    private String httpBody;

    @Column(name = "http_headers", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, String> httpHeaders;

    @Override
    public void validate(JobType type) {
        if (type != JobType.HTTP) {
            throw new IllegalArgumentException("Invalid job type for HttpJobPayload: " + type);
        }
        if (httpUrl == null || httpUrl.isBlank()) {
            throw new IllegalArgumentException("HTTP URL is required for HTTP jobs");
        }
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("HTTP method is required for HTTP jobs");
        }
    }
}
