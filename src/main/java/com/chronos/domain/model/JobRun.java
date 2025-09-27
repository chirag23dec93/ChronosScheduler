package com.chronos.domain.model;

import com.chronos.domain.model.enums.JobOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRun {
    @Id
    private String id; // ULID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(nullable = false)
    private Integer attempt;

    @Enumerated(EnumType.STRING)
    private JobOutcome outcome;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "duration_ms")
    private Long durationMs;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JobRunLog> logs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (attempt == null) {
            attempt = 1;
        }
    }
}
