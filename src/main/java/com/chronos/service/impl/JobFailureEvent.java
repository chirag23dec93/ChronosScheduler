package com.chronos.service.impl;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobFailureEvent extends ApplicationEvent {
    private final Job job;
    private final JobRun run;
    private final String reason;

    public JobFailureEvent(Object source, Job job, JobRun run, String reason) {
        super(source);
        this.job = job;
        this.run = run;
        this.reason = reason;
    }
}
