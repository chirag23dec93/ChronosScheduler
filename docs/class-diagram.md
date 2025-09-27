# Chronos Job Scheduling System - Class Diagram

## Overview
This document provides a comprehensive UML class diagram for the Chronos job scheduling system, showing all key classes, their relationships, methods, and architectural patterns.

## Core Domain Model

```mermaid
classDiagram
    %% Core Domain Entities
    class User {
        -Long id
        -String email
        -String passwordHash
        -String roles
        -Instant createdAt
        +getId() Long
        +getEmail() String
        +getRoles() String
        +getAuthorities() Collection~GrantedAuthority~
    }

    class Job {
        -String id
        -User owner
        -String name
        -JobType type
        -JobStatus status
        -JobPriority priority
        -Instant createdAt
        -Instant lastRunAt
        -Instant nextRunAt
        -String workerId
        -JobSchedule schedule
        -JobPayload payload
        -RetryPolicy retryPolicy
        +getId() String
        +getName() String
        +getType() JobType
        +getStatus() JobStatus
        +onCreate() void
    }

    class JobSchedule {
        -Long id
        -Job job
        -ScheduleType scheduleType
        -Instant runAt
        -String cronExpression
        -Long intervalSeconds
        -String timezone
        -MisfirePolicy misfirePolicy
        +getScheduleType() ScheduleType
        +getRunAt() Instant
        +getCronExpression() String
    }

    class JobPayload {
        <<abstract>>
        -Job job
        -String payloadType
        +getPayloadType() String
        +validate() void
    }

    class HttpJobPayload {
        -String httpUrl
        -String httpMethod
        -Map~String,String~ httpHeaders
        -String httpBody
        -Integer timeoutSeconds
        +getHttpUrl() String
        +getHttpMethod() String
        +validate() void
    }

    class DatabaseJobPayload {
        -String databaseUrl
        -String query
        -Map~String,Object~ parameters
        -String transactionIsolation
        -Integer queryTimeoutSeconds
        -Integer maxRows
        -Boolean readOnly
        +getDatabaseUrl() String
        +getQuery() String
        +validate() void
    }

    class MessageQueueJobPayload {
        -String operationType
        -String queueName
        -String messageBody
        -String messageGroupId
        -Map~String,Object~ messageAttributes
        -Map~String,Object~ queueConfig
        -Integer batchSize
        -Integer visibilityTimeoutSeconds
        +getOperationType() String
        +getQueueName() String
        +validate() void
    }

    class DbToKafkaJobPayload {
        -String databaseUrl
        -String query
        -String kafkaTopic
        -String kafkaKeyField
        -Map~String,String~ kafkaHeaders
        -String offsetField
        -String lastProcessedValue
        -Map~String,String~ fieldMappings
        -List~String~ excludeFields
        -Boolean includeMetadata
        -String deadLetterTopic
        -Boolean skipOnError
        -Integer maxRetries
        -Integer maxRecords
        -Integer connectionTimeoutSeconds
        +getDatabaseUrl() String
        +getKafkaTopic() String
        +validate() void
    }

    class RetryPolicy {
        -Job job
        -Integer maxAttempts
        -BackoffStrategy backoffStrategy
        -Integer backoffSeconds
        -List~String~ retryOn
        +getMaxAttempts() Integer
        +getBackoffStrategy() BackoffStrategy
        +shouldRetry() Boolean
    }

    class JobRun {
        -String id
        -Job job
        -Instant scheduledTime
        -Instant startTime
        -Instant endTime
        -Integer attempt
        -JobOutcome outcome
        -Integer exitCode
        -String errorMessage
        -String workerId
        -Long durationMs
        +getId() String
        +getAttempt() Integer
        +getOutcome() JobOutcome
        +calculateDuration() Long
    }

    class JobRunLog {
        -Long id
        -JobRun run
        -Instant timestamp
        -String level
        -String message
        -Map~String,Object~ context
        +getId() Long
        +getLevel() String
        +getMessage() String
    }

    %% Enums
    class JobType {
        <<enumeration>>
        HTTP
        SCRIPT
        DATABASE
        FILE_SYSTEM
        MESSAGE_QUEUE
        DB_TO_KAFKA
        CACHE
        REPORT
        DUMMY
    }

    class JobStatus {
        <<enumeration>>
        PENDING
        SCHEDULED
        RUNNING
        SUCCEEDED
        FAILED
        CANCELLED
        PAUSED
    }

    class JobPriority {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
        CRITICAL
    }

    class ScheduleType {
        <<enumeration>>
        ONCE
        CRON
        INTERVAL
    }

    class BackoffStrategy {
        <<enumeration>>
        FIXED
        EXPONENTIAL
    }

    class JobOutcome {
        <<enumeration>>
        SUCCESS
        FAILURE
    }

    %% Relationships
    User ||--o{ Job : owns
    Job ||--|| JobSchedule : has
    Job ||--|| JobPayload : has
    Job ||--o| RetryPolicy : has
    Job ||--o{ JobRun : executes
    JobRun ||--o{ JobRunLog : generates
    JobPayload <|-- HttpJobPayload
    JobPayload <|-- DatabaseJobPayload
    JobPayload <|-- MessageQueueJobPayload
    JobPayload <|-- DbToKafkaJobPayload
    Job --> JobType
    Job --> JobStatus
    Job --> JobPriority
    JobSchedule --> ScheduleType
    RetryPolicy --> BackoffStrategy
    JobRun --> JobOutcome
```

## Service Layer Architecture

```mermaid
classDiagram
    %% Service Interfaces
    class JobService {
        <<interface>>
        +createJob(CreateJobRequest) JobResponse
        +getJob(String) JobResponse
        +getJobs(JobStatus, String, Instant, Instant, Pageable) Page~JobResponse~
        +updateJob(String, CreateJobRequest) JobResponse
        +deleteJob(String) void
        +scheduleJob(String, Instant) JobResponse
        +cancelJob(String) JobResponse
        +pauseJob(String) JobResponse
        +resumeJob(String) JobResponse
        +triggerJobNow(String) JobResponse
        +getJobRuns(String, Pageable) Page~JobRunSummaryDto~
        +getJobRun(String, String) JobRunSummaryDto
        +getJobRunLogs(String, String, Pageable) Page~JobRunLogDto~
        +findReadyJobs(int) List~JobResponse~
        +markJobAsRunning(String, String, String) void
        +markJobAsComplete(String, String, boolean, String) void
        +shouldRetry(String, int) boolean
        +calculateNextRetryTime(String, int) Instant
        +findJobOrThrow(String) Job
    }

    class JobExecutorService {
        <<interface>>
        +executeJob(Job, JobRun) void
        +execute(Job, JobRun) void
    }

    class QuartzSchedulerService {
        <<interface>>
        +initializeScheduler() void
        +shutdownScheduler() void
        +scheduleJob(Job) void
        +rescheduleJob(Job, Instant) void
        +rescheduleJob(Job, Instant, int) void
        +deleteJob(String) void
        +pauseJob(String) void
        +resumeJob(String) void
        +triggerJobNow(String) void
        +getNextFireTime(String) Instant
        +isJobScheduled(String) boolean
        +stopAllJobs() void
    }

    class NotificationService {
        <<interface>>
        +notifyJobCreation(Job) void
        +notifyJobCompletion(Job, JobRun) void
        +notifyJobFailure(Job, JobRun, String) void
        +notifyMaxRetriesExceeded(Job, JobRun) void
        +sendNotification(Notification) void
    }

    class AuditService {
        <<interface>>
        +auditEvent(String, String, String) void
        +auditEvent(String, String, String, Map) void
        +getAuditEvents(String, Pageable) Page~AuditEventDto~
    }

    class DLQService {
        <<interface>>
        +addToDLQ(Job, JobRun, String) void
        +getDLQEvents(Pageable) Page~DLQEventDto~
        +replayJob(Long) void
        +resolveEvent(Long) void
        +cleanupResolvedEvents() void
    }

    %% Service Implementations
    class JobServiceImpl {
        -JobRepository jobRepository
        -JobRunRepository jobRunRepository
        -JobRunLogRepository jobRunLogRepository
        -UserRepository userRepository
        -JobMapper jobMapper
        -QuartzSchedulerService quartzSchedulerService
        -NotificationService notificationService
        -AuditService auditService
        -ApplicationEventPublisher eventPublisher
        -JobMetrics jobMetrics
        +createJob(CreateJobRequest) JobResponse
        +markJobAsComplete(String, String, boolean, String) void
        +handleJobFailure(Job, JobRun, String) void
        -validateJobRequest(CreateJobRequest) void
        -validateJobOwnership(Job) void
        -getCurrentUser() User
    }

    class JobExecutorServiceImpl {
        -HttpJobExecutor httpJobExecutor
        -DatabaseJobExecutor databaseJobExecutor
        -MessageQueueJobExecutor messageQueueJobExecutor
        -DbToKafkaJobExecutor dbToKafkaJobExecutor
        -CacheJobExecutor cacheJobExecutor
        -FileSystemJobExecutor fileSystemJobExecutor
        -ReportJobExecutor reportJobExecutor
        -JobService jobService
        -ExecutorService executorService
        +executeJob(Job, JobRun) void
        +execute(Job, JobRun) void
        -getExecutorForJobType(JobType) JobTypeExecutor
    }

    class QuartzSchedulerServiceImpl {
        -Scheduler scheduler
        -ApplicationContext applicationContext
        +initializeScheduler() void
        +scheduleJob(Job) void
        +rescheduleJob(Job, Instant, int) void
        -createJobDetail(Job, int) JobDetail
        -createTrigger(Job, Instant) Trigger
    }

    %% Job Executors
    class JobTypeExecutor {
        <<interface>>
        +execute(Job, JobRun) void
        +validate(JobPayload) void
    }

    class HttpJobExecutor {
        -RestTemplate restTemplate
        -JobService jobService
        +execute(Job, JobRun) void
        +validate(JobPayload) void
        -buildHttpRequest(HttpJobPayload) HttpEntity
        -handleHttpResponse(ResponseEntity) void
    }

    class DatabaseJobExecutor {
        -DatabaseJobDataSourceFactory dataSourceFactory
        -JobService jobService
        +execute(Job, JobRun) void
        +validate(JobPayload) void
        -executeQuery(DatabaseJobPayload) ResultSet
        -processResults(ResultSet) List~Map~
    }

    class MessageQueueJobExecutor {
        -KafkaTemplate kafkaTemplate
        -RabbitTemplate rabbitTemplate
        -ApplicationContext applicationContext
        +execute(Job, JobRun) void
        +validate(JobPayload) void
        -executeProduceOperation(MessageQueueJobPayload) void
        -executeConsumeOperation(MessageQueueJobPayload) void
    }

    class DbToKafkaJobExecutor {
        -DatabaseJobDataSourceFactory dataSourceFactory
        -KafkaTemplate kafkaTemplate
        -JobService jobService
        +execute(Job, JobRun) void
        +validate(JobPayload) void
        -streamDataToKafka(DbToKafkaJobPayload) void
        -processRecordBatch(List, DbToKafkaJobPayload) void
    }

    %% Relationships
    JobService <|.. JobServiceImpl
    JobExecutorService <|.. JobExecutorServiceImpl
    QuartzSchedulerService <|.. QuartzSchedulerServiceImpl
    JobTypeExecutor <|.. HttpJobExecutor
    JobTypeExecutor <|.. DatabaseJobExecutor
    JobTypeExecutor <|.. MessageQueueJobExecutor
    JobTypeExecutor <|.. DbToKafkaJobExecutor
    JobServiceImpl --> JobRepository
    JobServiceImpl --> QuartzSchedulerService
    JobServiceImpl --> NotificationService
    JobServiceImpl --> AuditService
    JobExecutorServiceImpl --> JobTypeExecutor
    JobExecutorServiceImpl --> JobService
```

## Repository Layer

```mermaid
classDiagram
    %% Repository Interfaces
    class JobRepository {
        <<interface>>
        +findByOwner(User, Pageable) Page~Job~
        +findByOwnerAndStatus(User, JobStatus, Pageable) Page~Job~
        +findByOwnerAndNameContaining(User, String, Pageable) Page~Job~
        +findByOwnerAndNextRunBetween(User, Instant, Instant, Pageable) Page~Job~
        +findReadyToRun(JobStatus, Instant, Pageable) Page~Job~
        +updateJobStatus(String, JobStatus) int
        +updateAllJobStatus(JobStatus) void
    }

    class JobRunRepository {
        <<interface>>
        +findByJob(Job, Pageable) Page~JobRun~
        +findByJobAndAttempt(Job, int) Optional~JobRun~
        +findByIdAndJobId(String, String) Optional~JobRun~
        +findFirstByJobIdOrderByStartTimeDesc(String) Optional~JobRun~
        +existsByJobIdAndAttempt(String, int) boolean
        +countByJobAndOutcome(Job, JobOutcome) long
    }

    class JobRunLogRepository {
        <<interface>>
        +findByRun(JobRun, Pageable) Page~JobRunLog~
        +findByRunOrderByTimestampAsc(JobRun) List~JobRunLog~
        +deleteByRunId(String) void
    }

    class UserRepository {
        <<interface>>
        +findByEmail(String) Optional~User~
        +existsByEmail(String) boolean
    }

    class NotificationRepository {
        <<interface>>
        +findByJobId(String) List~Notification~
        +findByUserId(Long) List~Notification~
        +findBySentAtIsNull() List~Notification~
    }

    class DLQEventRepository {
        <<interface>>
        +findByJobId(String) List~DLQEvent~
        +findByCreatedAtBefore(Instant) List~DLQEvent~
        +deleteResolvedEvents() void
    }

    class AuditEventRepository {
        <<interface>>
        +findByUserId(Long, Pageable) Page~AuditEvent~
        +findByEntityTypeAndEntityId(String, String) List~AuditEvent~
        +deleteOldEvents(Instant) void
    }

    %% Spring Data JPA
    class JpaRepository {
        <<interface>>
        +save(T) T
        +findById(ID) Optional~T~
        +findAll() List~T~
        +delete(T) void
        +deleteById(ID) void
    }

    %% Relationships
    JpaRepository <|-- JobRepository
    JpaRepository <|-- JobRunRepository
    JpaRepository <|-- JobRunLogRepository
    JpaRepository <|-- UserRepository
    JpaRepository <|-- NotificationRepository
    JpaRepository <|-- DLQEventRepository
    JpaRepository <|-- AuditEventRepository
```

## API Layer

```mermaid
classDiagram
    %% Controllers
    class JobController {
        -JobService jobService
        +createJob(CreateJobRequest) ResponseEntity~JobResponse~
        +getJob(String) ResponseEntity~JobResponse~
        +getJobs(JobStatus, String, Instant, Instant, Pageable) ResponseEntity~Page~
        +updateJob(String, CreateJobRequest) ResponseEntity~JobResponse~
        +deleteJob(String) ResponseEntity~Void~
        +scheduleJob(String, Instant) ResponseEntity~JobResponse~
        +cancelJob(String) ResponseEntity~JobResponse~
        +pauseJob(String) ResponseEntity~JobResponse~
        +resumeJob(String) ResponseEntity~JobResponse~
        +triggerJobNow(String) ResponseEntity~JobResponse~
        +getJobRuns(String, Pageable) ResponseEntity~Page~
        +getJobRun(String, String) ResponseEntity~JobRunSummaryDto~
        +getJobRunLogs(String, String, Pageable) ResponseEntity~Page~
    }

    class AuthController {
        -AuthenticationService authService
        +register(RegisterRequest) ResponseEntity~AuthResponse~
        +login(LoginRequest) ResponseEntity~AuthResponse~
        +refreshToken(RefreshTokenRequest) ResponseEntity~AuthResponse~
    }

    class NotificationController {
        -NotificationService notificationService
        +getNotifications(Pageable) ResponseEntity~Page~
        +markAsRead(Long) ResponseEntity~Void~
        +updateSettings(NotificationSettingsRequest) ResponseEntity~Void~
    }

    class DLQController {
        -DLQService dlqService
        +getDLQEvents(Pageable) ResponseEntity~Page~
        +replayJob(Long) ResponseEntity~Void~
        +resolveEvent(Long) ResponseEntity~Void~
    }

    class AuditController {
        -AuditService auditService
        +getAuditEvents(String, Pageable) ResponseEntity~Page~
        +getEntityHistory(String, String) ResponseEntity~List~
    }

    %% DTOs
    class CreateJobRequest {
        -String name
        -JobType type
        -JobPriority priority
        -JobScheduleDto schedule
        -JobPayloadDto payload
        -RetryPolicyDto retryPolicy
        +getName() String
        +getType() JobType
        +validate() void
    }

    class JobResponse {
        -String id
        -String name
        -JobType type
        -JobStatus status
        -JobPriority priority
        -String ownerEmail
        -Instant createdAt
        -JobScheduleDto schedule
        -JobPayloadDto payload
        -RetryPolicyDto retryPolicy
        +getId() String
        +getStatus() JobStatus
    }

    class JobRunSummaryDto {
        -String id
        -String jobId
        -Instant scheduledTime
        -Instant startTime
        -Instant endTime
        -Integer attempt
        -JobOutcome outcome
        -String errorMessage
        -String workerId
        -Long durationMs
        +getId() String
        +getOutcome() JobOutcome
    }

    %% Mappers
    class JobMapper {
        <<interface>>
        +toJob(CreateJobRequest) Job
        +toJobResponse(Job) JobResponse
        +toJobRunSummaryDto(JobRun) JobRunSummaryDto
        +updateJobFromRequest(Job, CreateJobRequest) void
    }

    %% Relationships
    JobController --> JobService
    JobController --> CreateJobRequest
    JobController --> JobResponse
    AuthController --> AuthenticationService
    NotificationController --> NotificationService
    DLQController --> DLQService
    AuditController --> AuditService
    JobMapper --> Job
    JobMapper --> JobResponse
```

This comprehensive class diagram shows the complete architecture of the Chronos job scheduling system, including:

1. **Domain Model**: Core entities with their relationships and methods
2. **Service Layer**: Business logic interfaces and implementations
3. **Repository Layer**: Data access layer with Spring Data JPA
4. **API Layer**: REST controllers, DTOs, and mappers
5. **Job Executors**: Specialized executors for different job types

The diagram demonstrates key architectural patterns like:
- **Layered Architecture**: Clear separation between API, Service, and Repository layers
- **Strategy Pattern**: JobTypeExecutor interface with multiple implementations
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Data transfer objects for API communication
- **Dependency Injection**: Service dependencies managed by Spring
- **Polymorphism**: JobPayload hierarchy for different job types
