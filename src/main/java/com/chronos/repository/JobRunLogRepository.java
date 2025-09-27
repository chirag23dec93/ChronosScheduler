package com.chronos.repository;

import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.JobRunLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JobRunLogRepository extends JpaRepository<JobRunLog, Long> {
    
    Page<JobRunLog> findByRun(JobRun run, Pageable pageable);
    
    @Query("SELECT l FROM JobRunLog l WHERE l.run = :run AND l.level = :level")
    Page<JobRunLog> findByRunAndLevel(JobRun run, String level, Pageable pageable);
    
    @Query("SELECT l FROM JobRunLog l WHERE l.run = :run AND l.timestamp >= :since")
    List<JobRunLog> findByRunSince(JobRun run, Instant since);
    
    @Query("SELECT l FROM JobRunLog l WHERE l.run = :run AND l.message LIKE %:search%")
    Page<JobRunLog> searchInRunLogs(JobRun run, String search, Pageable pageable);
    
    void deleteByRunAndTimestampBefore(JobRun run, Instant before);
}
