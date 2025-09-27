package com.chronos.domain.model;

import com.chronos.domain.model.enums.BackoffStrategy;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "retry_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryPolicy {
    @Id
    @Column(name = "job_id")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "backoff_strategy", nullable = false)
    private BackoffStrategy backoffStrategy;

    @Column(name = "backoff_seconds", nullable = false)
    private Integer backoffSeconds;

    @Column(columnDefinition = "json")
    @Type(JsonType.class)
    private List<String> retryOn;

    @PrePersist
    protected void onCreate() {
        if (maxAttempts == null) {
            maxAttempts = 3;
        }
        if (backoffSeconds == null) {
            backoffSeconds = 60;
        }
        if (retryOn == null) {
            retryOn = List.of("5xx", "timeout", "non2xx", "exception");
        }
    }
}
