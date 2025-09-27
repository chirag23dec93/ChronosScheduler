package com.chronos.domain.model;

import com.chronos.domain.model.enums.MisfirePolicy;
import com.chronos.domain.model.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "job_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    @JsonBackReference
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "run_at")
    private Instant runAt;

    @Column(name = "cron_expr")
    private String cronExpression;

    @Column(name = "interval_seconds")
    private Long intervalSeconds;

    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "misfire_policy", nullable = false)
    private MisfirePolicy misfirePolicy;

    @PrePersist
    protected void onCreate() {
        if (timezone == null) {
            timezone = "UTC";
        }
    }
}
