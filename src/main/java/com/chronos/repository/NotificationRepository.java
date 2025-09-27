package com.chronos.repository;

import com.chronos.domain.model.Job;
import com.chronos.domain.model.Notification;
import com.chronos.domain.model.User;
import com.chronos.domain.model.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUser(User user, Pageable pageable);
    
    Page<Notification> findByJob(Job job, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.sentAt IS NULL")
    List<Notification> findPendingNotifications();
    
    @Query("SELECT n FROM Notification n " +
           "WHERE n.channel = :channel AND n.sentAt IS NULL")
    List<Notification> findPendingNotificationsByChannel(NotificationChannel channel);
    
    @Query("SELECT COUNT(n) FROM Notification n " +
           "WHERE n.user = :user AND n.sentAt >= :since")
    long countRecentNotifications(User user, Instant since);
    
    void deleteByJobAndUser(Job job, User user);
}
