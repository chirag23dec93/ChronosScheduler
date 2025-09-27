package com.chronos.repository;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.User;
import com.chronos.domain.model.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String>, JpaSpecificationExecutor<Job> {
    
    Page<Job> findByOwner(User owner, Pageable pageable);
    
    Page<Job> findByOwnerAndStatus(User owner, JobStatus status, Pageable pageable);
    
    @Query("SELECT j FROM Job j WHERE j.owner = :owner AND j.name LIKE %:nameContains%")
    Page<Job> findByOwnerAndNameContaining(User owner, String nameContains, Pageable pageable);
    
    @Query("SELECT j FROM Job j " +
           "LEFT JOIN j.schedule s " +
           "WHERE j.owner = :owner " +
           "AND (s.runAt BETWEEN :from AND :to OR s.cronExpression IS NOT NULL)")
    Page<Job> findByOwnerAndNextRunBetween(User owner, Instant from, Instant to, Pageable pageable);
    
    Optional<Job> findByIdAndOwner(String id, User owner);
    
    boolean existsByNameAndOwner(String name, User owner);
    
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Job j SET j.status = :status WHERE j.id = :jobId")
    @Transactional
    int updateJobStatus(String jobId, JobStatus status);
    
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Job j SET j.status = :status WHERE j.status != :status")
    @Transactional
    int updateAllJobStatus(JobStatus status);
    
    @Query("SELECT j FROM Job j " +
           "WHERE j.status = :status " +
           "AND j.schedule.runAt <= :now " +
           "ORDER BY j.priority DESC, j.createdAt ASC")
    List<Job> findReadyToRun(JobStatus status, Instant now, Pageable pageable);
    
    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = :status")
    long countByStatus(JobStatus status);
}
