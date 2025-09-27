package com.chronos.repository;

import com.chronos.domain.model.AuditEvent;
import com.chronos.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    
    Page<AuditEvent> findByUser(User user, Pageable pageable);
    
    @Query("SELECT e FROM AuditEvent e WHERE e.entityType = :entityType AND e.entityId = :entityId")
    Page<AuditEvent> findByEntity(String entityType, String entityId, Pageable pageable);
    
    @Query("SELECT e FROM AuditEvent e WHERE e.action = :action")
    Page<AuditEvent> findByAction(String action, Pageable pageable);
    
    @Query("SELECT e FROM AuditEvent e " +
           "WHERE e.createdAt >= :from AND e.createdAt <= :to")
    List<AuditEvent> findBetween(Instant from, Instant to);
    
    @Query("SELECT COUNT(e) FROM AuditEvent e " +
           "WHERE e.user = :user AND e.action = :action AND e.createdAt >= :since")
    long countRecentActionsByUser(User user, String action, Instant since);
    
    @Query("SELECT e FROM AuditEvent e " +
           "WHERE e.entityType = :entityType " +
           "AND e.entityId = :entityId " +
           "ORDER BY e.createdAt DESC")
    List<AuditEvent> getEntityHistory(String entityType, String entityId);
    
    void deleteByCreatedAtBefore(Instant before);
}
