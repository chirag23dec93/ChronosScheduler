package com.chronos.repository;

import com.chronos.domain.model.DLQEvent;
import com.chronos.domain.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DLQEventRepository extends JpaRepository<DLQEvent, Long> {
    
    Page<DLQEvent> findByJob(Job job, Pageable pageable);
    
    @Query("SELECT e FROM DLQEvent e WHERE e.createdAt >= :since")
    List<DLQEvent> findRecentEvents(Instant since);
    
    @Query("SELECT COUNT(e) FROM DLQEvent e WHERE e.job = :job")
    long countByJob(Job job);
    
    @Query("SELECT e FROM DLQEvent e " +
           "WHERE e.reason LIKE %:reasonContains% " +
           "ORDER BY e.createdAt DESC")
    Page<DLQEvent> searchByReason(String reasonContains, Pageable pageable);
    
    void deleteByJobAndCreatedAtBefore(Job job, Instant before);
    
    @Query("SELECT e FROM DLQEvent e " +
           "WHERE NOT EXISTS (" +
           "    SELECT 1 FROM JobRun r " +
           "    WHERE r.job = e.job " +
           "    AND r.startTime > e.createdAt" +
           ")")
    List<DLQEvent> findUnresolvedEvents();
}
