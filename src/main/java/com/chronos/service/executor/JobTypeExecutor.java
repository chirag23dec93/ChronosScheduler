package com.chronos.service.executor;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;

public interface JobTypeExecutor {
    void execute(Job job, JobRun run);
}
