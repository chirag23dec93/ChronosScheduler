package com.chronos.repository;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.enums.JobOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRunRepository extends JpaRepository<JobRun, String> {
    Optional<JobRun> findByIdAndJobId(String id, String jobId);
    
    Page<JobRun> findByJob(Job job, Pageable pageable);
    
    Page<JobRun> findByJobAndOutcome(Job job, JobOutcome outcome, Pageable pageable);
    
    Optional<JobRun> findFirstByJobOrderByStartTimeDesc(Job job);
    
    Optional<JobRun> findFirstByJobIdOrderByStartTimeDesc(String jobId);
    
    Page<JobRun> findByJobId(String jobId, Pageable pageable);
    
    List<JobRun> findByJobIdAndOutcome(String jobId, JobOutcome outcome);
    
    List<JobRun> findByJobIdAndEndTimeIsNull(String jobId);
    
    @Query("SELECT jr FROM JobRun jr WHERE jr.job = :job AND jr.attempt = :attempt")
    Optional<JobRun> findByJobAndAttempt(Job job, Integer attempt);
    
    @Query("SELECT CASE WHEN COUNT(jr) > 0 THEN true ELSE false END FROM JobRun jr WHERE jr.job.id = :jobId AND jr.attempt = :attempt")
    boolean existsByJobIdAndAttempt(String jobId, Integer attempt);
    
    @Query("SELECT COUNT(jr) FROM JobRun jr WHERE jr.job = :job AND jr.outcome = :outcome")
    long countByJobAndOutcome(Job job, JobOutcome outcome);
    
    @Query("SELECT jr FROM JobRun jr WHERE jr.job = :job AND jr.startTime >= :since")
    List<JobRun> findByJobSince(Job job, Instant since);
    
    @Query("SELECT AVG(jr.durationMs) FROM JobRun jr " +
           "WHERE jr.job = :job AND jr.outcome = :outcome AND jr.startTime >= :since")
    Double getAverageDuration(Job job, JobOutcome outcome, Instant since);
    
    @Query("SELECT jr FROM JobRun jr " +
           "WHERE jr.endTime IS NULL AND jr.startTime < :timeout")
    List<JobRun> findStuckRuns(Instant timeout);
}
