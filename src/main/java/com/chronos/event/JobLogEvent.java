package com.chronos.event;

import com.chronos.domain.model.JobRun;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class JobLogEvent extends ApplicationEvent {
    private final JobRun run;
    private final String level;
    private final String message;
    private final Map<String, Object> context;

    public JobLogEvent(Object source, JobRun run, String level, String message) {
        this(source, run, level, message, null);
    }

    public JobLogEvent(Object source, JobRun run, String level, String message, Map<String, Object> context) {
        super(source);
        this.run = run;
        this.level = level;
        this.message = message;
        this.context = context;
    }
}
