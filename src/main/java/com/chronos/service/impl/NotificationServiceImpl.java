package com.chronos.service.impl;

import com.chronos.api.dto.notification.CreateNotificationRequest;
import com.chronos.api.dto.notification.NotificationResponse;
import com.chronos.api.mapper.NotificationMapper;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.domain.model.Notification;
import com.chronos.domain.model.enums.JobType;
import com.chronos.domain.model.enums.NotificationChannel;
import com.chronos.exception.ResourceNotFoundException;
import com.chronos.repository.JobRepository;
import com.chronos.repository.NotificationRepository;
import com.chronos.service.NotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final JobRepository jobRepository;
    private final NotificationMapper notificationMapper;
    private final SpringTemplateEngine templateEngine;
    private final RestTemplate restTemplate;

    @Value("${app.notification.email.admin-email}")
    private String adminEmail;

    private String getTypeStyle(JobType type) {
        switch (type) {
            case HTTP:
                return "background-color: #f8d7da; color: #721c24;";
            case SCRIPT:
                return "background-color: #fff3cd; color: #856404;";
            case DUMMY:
                return "background-color: #d4edda; color: #155724;";
            default:
                return "";
        }
    }

    @Override
    @Async
    @Transactional
    public void notifyJobCreation(Job job) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(adminEmail);
            helper.setSubject("New Job Created: " + job.getName());
            
            String simpleHtml = String.format(
                "<html><body>" +
                "<h2 style='color: #333; font-family: Arial, sans-serif;'>New Job Created</h2>" +
                "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin-bottom: 20px;'>" +
                "<p>Job: <strong>%s</strong></p>" +
                "<p>Type: <span style='font-weight: bold; padding: 3px 8px; border-radius: 3px; %s'>%s</span></p>" +
                "</div>" +
                "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px;'>" +
                "<h3 style='margin-top: 0;'>Schedule Details</h3>" +
                "<p>Schedule Type: %s</p>" +
                "<p>Run At: %s</p>" +
                "<p>Timezone: %s</p>" +
                "</div>" +
                "</body></html>",
                job.getName(),
                getTypeStyle(job.getType()), job.getType(),
                job.getSchedule().getScheduleType(),
                job.getSchedule().getRunAt(),
                job.getSchedule().getTimezone()
            );
            helper.setText(simpleHtml, true);
            
            mailSender.send(message);
            log.info("Sent job creation notification to admin for job {}", job.getId());
        } catch (Exception e) {
            log.error("Failed to send admin notification for job creation {}", job.getId(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void notifyJobCompletion(Job job, JobRun run) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(adminEmail);
            helper.setSubject("Job Completed: " + job.getName());
            
            String simpleHtml = String.format(
                "<html><body>" +
                "<h2 style='color: #333; font-family: Arial, sans-serif;'>Job Execution Successful</h2>" +
                "<p>Job <strong>%s</strong> has completed successfully.</p>" +
                "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px;'>" +
                "<h3 style='margin-top: 0;'>Execution Details</h3>" +
                "<ul style='list-style-type: none; padding-left: 0;'>" +
                "<li>Run ID: <span>%s</span></li>" +
                "<li>Start Time: <span>%s</span></li>" +
                "<li>End Time: <span>%s</span></li>" +
                "<li>Duration: <span>%d ms</span></li>" +
                "</ul>" +
                "</div>" +
                "</body></html>",
                job.getName(),
                run.getId(),
                run.getStartTime(),
                run.getEndTime(),
                run.getDurationMs()
            );
            helper.setText(simpleHtml, true);
            
            mailSender.send(message);
            log.info("Sent job completion notification to admin for job {}", job.getId());
        } catch (Exception e) {
            log.error("Failed to send admin notification for job completion {}", job.getId(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void notifyJobFailure(Job job, JobRun run, String reason) {
        Map<String, Object> context = new HashMap<>();
        context.put("jobName", job.getName());
        context.put("runId", run.getId());
        context.put("error", reason);
        context.put("attempt", run.getAttempt());
        context.put("maxAttempts", job.getRetryPolicy().getMaxAttempts());
        context.put("startTime", run.getStartTime());
        context.put("endTime", run.getEndTime());
        context.put("duration", run.getDurationMs());
        context.put("jobType", job.getType());
        context.put("priority", job.getPriority());
        
        // Send to configured notifications
        notificationRepository.findByJob(job, Pageable.unpaged())
                .forEach(notification -> {
                    notification.setPayload(context);
                    sendNotification(notification.getId(), context);
                });
        
        // Always send to admin
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(adminEmail);
            helper.setSubject(String.format("Job Failed (Attempt %d/%d): %s", run.getAttempt(), job.getRetryPolicy().getMaxAttempts(), job.getName()));
            
            Context thymeleafContext = new Context();
            thymeleafContext.setVariables(context);
            String htmlContent = templateEngine.process("mail/job-failure.html", thymeleafContext);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send admin notification for job failure {}", job.getId(), e);
        }
    }

    @Override
    @Async
    @Transactional
    public void notifyMaxRetriesExceeded(Job job, JobRun lastRun) {
        Map<String, Object> context = new HashMap<>();
        context.put("jobName", job.getName());
        context.put("runId", lastRun.getId());
        context.put("error", lastRun.getErrorMessage());
        context.put("attempts", lastRun.getAttempt());
        context.put("maxAttempts", job.getRetryPolicy().getMaxAttempts());
        
        notificationRepository.findByJob(job, Pageable.unpaged())
                .forEach(notification -> {
                    notification.setPayload(context);
                    sendNotification(notification.getId(), context);
                });
    }

    @Override
    @Async
    @Transactional
    public void sendNotification(Long notificationId, Map<String, Object> context) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Notification", notificationId.toString()));
        
        try {
            if (notification.getChannel() == NotificationChannel.EMAIL) {
                sendEmailNotification(notification, context);
            } else if (notification.getChannel() == NotificationChannel.WEBHOOK) {
                sendWebhookNotification(notification, context);
            }
            
            notification.setSentAt(Instant.now());
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send notification {}", notificationId, e);
        }
    }

    private void sendEmailNotification(Notification notification, Map<String, Object> context) 
            throws MessagingException {
        Context thymeleafContext = new Context();
        thymeleafContext.setVariables(context);
        
        String template = context.get("error") != null ? "mail/job-failure.html" : "mail/job-success.html";
        String htmlContent = templateEngine.process(template, thymeleafContext);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(notification.getTarget());
        helper.setSubject("Job " + (context.get("error") != null ? "Failed" : "Completed") + ": " + context.get("jobName"));
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }

    private void sendWebhookNotification(Notification notification, Map<String, Object> context) {
        restTemplate.postForEntity(notification.getTarget(), context, String.class);
    }

    @Override
    @Transactional
    public NotificationResponse addNotification(String jobId, CreateNotificationRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Job", jobId));

        Notification notification = notificationMapper.toNotification(request);
        notification.setJob(job);
        notification = notificationRepository.save(notification);

        return notificationMapper.toNotificationResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getJobNotifications(String jobId, Pageable pageable) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Job", jobId));

        return notificationRepository.findByJob(job, pageable)
                .map(notificationMapper::toNotificationResponse);
    }

    @Override
    @Transactional
    public void deleteNotification(String jobId, Long notificationId) {
        // Verify job exists
        jobRepository.findById(jobId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Job", jobId));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Notification", notificationId.toString()));

        if (!notification.getJob().getId().equals(jobId)) {
            throw ResourceNotFoundException.forResource("Notification", notificationId.toString());
        }

        notificationRepository.delete(notification);
    }
}
