package com.chronos.service;

import com.chronos.api.dto.dlq.DLQEventResponse;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DLQService {
    
    void addToDLQ(Job job, JobRun lastRun, String reason);
    
    Page<DLQEventResponse> getDLQEvents(Pageable pageable);
    
    Page<DLQEventResponse> searchDLQEvents(String reasonContains, Pageable pageable);
    
    DLQEventResponse getDLQEvent(Long eventId);
    
    void replayDLQEvent(Long eventId);
    
    void replayAllDLQEvents();
    
    void cleanupResolvedEvents();
}
