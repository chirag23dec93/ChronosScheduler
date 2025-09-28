# 🏗️ Chronos UML Class Diagrams

## 📦 Domain Model Class Diagram

```mermaid
classDiagram
    class User {
        -Long id
        -String email
        -String password
        -Set~Role~ roles
        -Instant createdAt
        -Instant updatedAt
        +getAuthorities() Collection~GrantedAuthority~
        +getUsername() String
        +isAccountNonExpired() boolean
        +isAccountNonLocked() boolean
        +isCredentialsNonExpired() boolean
        +isEnabled() boolean
    }

    class Job {
        -String id
        -User owner
        -String name
        -JobType type
        -JobStatus status
        -JobPriority priority
        -JobSchedule schedule
        -JobPayload payload
        -RetryPolicy retryPolicy
        -List~JobRun~ runs
        -List~Notification~ notifications
        -Instant createdAt
        -Instant updatedAt
        +getId() String
        +setStatus(JobStatus) void
        +addRun(JobRun) void
    }

    class JobSchedule {
        -Long id
        -ScheduleType scheduleType
        -Instant runAt
        -String cronExpression
        -Long intervalSeconds
        -String timezone
        -MisfirePolicy misfirePolicy
        +getNextExecutionTime() Instant
        +isRecurring() boolean
    }

    class JobPayload {
        <<abstract>>
        -Long id
        -String type
        -Map~String,Object~ metadata
        +validate() void
        +getExecutionParameters() Map~String,Object~
    }

    class HttpJobPayload {
        -String httpUrl
        -String httpMethod
        -Map~String,String~ httpHeaders
        -String httpBody
        -Integer timeoutSeconds
        +buildRequest() HttpRequest
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
        +getDataSource() DataSource
        +validate() void
    }

    class MessageQueueJobPayload {
        -String queueName
        -String operationType
        -String messageBody
        -String messageGroupId
        -Map~String,Object~ queueConfig
        -Integer batchSize
        +getQueueConfiguration() QueueConfig
        +validate() void
    }

    class RetryPolicy {
        -Long id
        -Integer maxAttempts
        -BackoffStrategy backoffStrategy
        -Integer backoffSeconds
        -List~String~ retryOn
        +shouldRetry(Exception) boolean
        +getNextRetryDelay(int) Duration
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
        -List~JobRunLog~ logs
        +isCompleted() boolean
        +getDuration() Duration
        +addLog(String,String) void
    }

    class JobRunLog {
        -Long id
        -String runId
        -Instant timestamp
        -String level
        -String message
        -Map~String,Object~ context
        +getFormattedMessage() String
    }

    class Notification {
        -Long id
        -User user
        -Job job
        -NotificationChannel channel
        -String target
        -String templateCode
        -Map~String,Object~ payload
        -NotificationStatus status
        -Instant createdAt
        -Instant sentAt
        +send() void
        +markAsSent() void
    }

    class DLQEvent {
        -String id
        -Job job
        -JobRun run
        -String failureReason
        -DLQStatus status
        -Instant createdAt
        -Instant resolvedAt
        -String resolutionNotes
        +resolve(String) void
        +replay() void
    }

    class AuditEvent {
        -String id
        -String entityType
        -String entityId
        -String action
        -String userEmail
        -Instant timestamp
        -Map~String,Object~ details
        +getEntityReference() String
    }

    %% Enums
    class JobType {
        <<enumeration>>
        HTTP
        DATABASE
        FILE_SYSTEM
        MESSAGE_QUEUE
        CACHE
        EMAIL
        REPORT
        SCRIPT
        DUMMY
        DB_TO_KAFKA
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
    }

    class ScheduleType {
        <<enumeration>>
        ONCE
        CRON
        INTERVAL
    }

    class JobOutcome {
        <<enumeration>>
        SUCCESS
        FAILURE
        TIMEOUT
        CANCELLED
    }

    %% Relationships
    User ||--o{ Job : owns
    Job ||--|| JobSchedule : has
    Job ||--|| JobPayload : contains
    Job ||--o| RetryPolicy : defines
    Job ||--o{ JobRun : executes
    Job ||--o{ Notification : triggers
    Job ||--o{ DLQEvent : fails
    JobRun ||--o{ JobRunLog : generates
    JobRun ||--o| DLQEvent : creates
    User ||--o{ Notification : receives
    
    JobPayload <|-- HttpJobPayload
    JobPayload <|-- DatabaseJobPayload
    JobPayload <|-- MessageQueueJobPayload
    
    Job --> JobType
    Job --> JobStatus
    Job --> JobPriority
    JobSchedule --> ScheduleType
    JobRun --> JobOutcome
```

## 🔧 Service Layer Class Diagram

```mermaid
classDiagram
    class JobService {
        <<interface>>
        +createJob(Job) Job
        +getJobById(String) Job
        +getAllJobs(Pageable) Page~Job~
        +pauseJob(String) Job
        +resumeJob(String) Job
        +cancelJob(String) Job
        +deleteJob(String) void
        +getJobRuns(String,Pageable) Page~JobRun~
        +getJobRunLogs(String,String,Pageable) Page~JobRunLog~
    }

    class JobServiceImpl {
        -JobRepository jobRepository
        -JobRunRepository jobRunRepository
        -JobRunLogRepository jobRunLogRepository
        -QuartzSchedulerService schedulerService
        -AuditService auditService
        -JobMetrics jobMetrics
        +createJob(Job) Job
        +pauseJob(String) Job
        +resumeJob(String) Job
        +cancelJob(String) Job
        +markJobAsRunning(String,String,String) void
        +markJobAsComplete(String,String,boolean,String) void
        -validateJobOwnership(Job) void
    }

    class JobExecutorService {
        <<interface>>
        +executeJob(String) void
        +executeJob(Job,JobRun) void
    }

    class JobExecutorServiceImpl {
        -Map~JobType,JobTypeExecutor~ executors
        -JobService jobService
        -ApplicationEventPublisher eventPublisher
        +executeJob(String) void
        +executeJob(Job,JobRun) void
        -getExecutor(JobType) JobTypeExecutor
    }

    class JobTypeExecutor {
        <<interface>>
        +getSupportedType() JobType
        +execute(Job,JobRun) void
    }

    class HttpJobExecutor {
        -RestTemplate restTemplate
        -ObjectMapper objectMapper
        -ApplicationEventPublisher eventPublisher
        +getSupportedType() JobType
        +execute(Job,JobRun) void
        -buildHttpRequest(HttpJobPayload) HttpEntity
        -handleResponse(ResponseEntity) void
    }

    class DatabaseJobExecutor {
        -DatabaseJobDataSourceFactory dataSourceFactory
        -ObjectMapper objectMapper
        -ApplicationEventPublisher eventPublisher
        +getSupportedType() JobType
        +execute(Job,JobRun) void
        -executeQuery(DatabaseJobPayload) Object
        -executeUpdate(DatabaseJobPayload) int
    }

    class MessageQueueJobExecutor {
        -KafkaTemplate kafkaTemplate
        -ObjectMapper objectMapper
        -ApplicationContext applicationContext
        -ApplicationEventPublisher eventPublisher
        +getSupportedType() JobType
        +execute(Job,JobRun) void
        -produceMessage(MessageQueueJobPayload) void
        -consumeMessages(MessageQueueJobPayload) void
    }

    class QuartzSchedulerService {
        <<interface>>
        +scheduleJob(Job) void
        +pauseJob(String) void
        +resumeJob(String) void
        +deleteJob(String) void
    }

    class QuartzSchedulerServiceImpl {
        -Scheduler scheduler
        -JobExecutorService jobExecutorService
        +scheduleJob(Job) void
        +pauseJob(String) void
        +resumeJob(String) void
        +deleteJob(String) void
        -createJobDetail(Job) JobDetail
        -createTrigger(JobSchedule) Trigger
    }

    class NotificationService {
        <<interface>>
        +sendJobCompletionNotification(Job) void
        +sendJobFailureNotification(Job,String) void
        +createEmailTemplate(String,Map) String
    }

    class NotificationServiceImpl {
        -NotificationRepository notificationRepository
        -JavaMailSender mailSender
        -RestTemplate restTemplate
        -TemplateEngine templateEngine
        -AuditService auditService
        +sendEmailNotification(Notification,Map) void
        +sendWebhookNotification(Notification,Map) void
        +batchProcessNotifications(int) void
    }

    %% Relationships
    JobService <|.. JobServiceImpl
    JobExecutorService <|.. JobExecutorServiceImpl
    QuartzSchedulerService <|.. QuartzSchedulerServiceImpl
    NotificationService <|.. NotificationServiceImpl
    
    JobTypeExecutor <|.. HttpJobExecutor
    JobTypeExecutor <|.. DatabaseJobExecutor
    JobTypeExecutor <|.. MessageQueueJobExecutor
    
    JobServiceImpl --> QuartzSchedulerService
    JobExecutorServiceImpl --> JobTypeExecutor
    QuartzSchedulerServiceImpl --> JobExecutorService
```

## 🌐 API Layer Class Diagram

```mermaid
classDiagram
    class JobController {
        -JobService jobService
        -JobMapper jobMapper
        +createJob(CreateJobRequest) ResponseEntity~JobResponse~
        +getJob(String) ResponseEntity~JobResponse~
        +getAllJobs(Pageable) ResponseEntity~Page~JobResponse~~
        +pauseJob(String) ResponseEntity~JobResponse~
        +resumeJob(String) ResponseEntity~JobResponse~
        +cancelJob(String) ResponseEntity~JobResponse~
        +deleteJob(String) ResponseEntity~Void~
        +getJobRuns(String,Pageable) ResponseEntity~Page~JobRunSummaryDto~~
        +getJobRunLogs(String,String,Pageable) ResponseEntity~Page~JobRunLogDto~~
    }

    class AuthController {
        -AuthenticationService authService
        +register(AuthRequest) ResponseEntity~AuthResponse~
        +login(AuthRequest) ResponseEntity~AuthResponse~
        +refreshToken(String) ResponseEntity~AuthResponse~
        +logout() ResponseEntity~Void~
    }

    class NotificationController {
        -NotificationService notificationService
        -NotificationMapper notificationMapper
        +createNotification(CreateNotificationRequest) ResponseEntity~NotificationResponse~
        +getNotifications(Pageable) ResponseEntity~Page~NotificationResponse~~
        +deleteNotification(Long) ResponseEntity~Void~
    }

    class DLQController {
        -DLQService dlqService
        -DLQEventMapper dlqEventMapper
        +getDLQEvents(Pageable) ResponseEntity~Page~DLQEventResponse~~
        +replayJob(String) ResponseEntity~Void~
        +resolveEvent(String,String) ResponseEntity~Void~
    }

    class AuditController {
        -AuditService auditService
        -AuditEventMapper auditEventMapper
        +getAuditEvents(Pageable) ResponseEntity~Page~AuditEventResponse~~
        +getEntityHistory(String,String,Pageable) ResponseEntity~Page~AuditEventResponse~~
        +getUserActivity(String,Pageable) ResponseEntity~Page~AuditEventResponse~~
    }

    class GlobalExceptionHandler {
        +handleValidationException(MethodArgumentNotValidException) ResponseEntity~ErrorResponse~
        +handleResourceNotFoundException(ResourceNotFoundException) ResponseEntity~ErrorResponse~
        +handleJobExecutionException(JobExecutionException) ResponseEntity~ErrorResponse~
        +handleAccessDeniedException(AccessDeniedException) ResponseEntity~ErrorResponse~
        +handleGenericException(Exception) ResponseEntity~ErrorResponse~
        -createErrorResponse(String,int,String) ErrorResponse
    }

    class JobMapper {
        <<interface>>
        +toEntity(CreateJobRequest) Job
        +toResponse(Job) JobResponse
        +toJobRunSummaryDto(JobRun) JobRunSummaryDto
        +toJobRunLogDto(JobRunLog) JobRunLogDto
        -validateJobPayload(JobPayloadDto) void
    }

    class SecurityConfig {
        -JwtAuthenticationFilter jwtAuthFilter
        -AuthenticationProvider authProvider
        +securityFilterChain(HttpSecurity) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager(AuthenticationConfiguration) AuthenticationManager
        +corsConfigurationSource() CorsConfigurationSource
    }

    class JwtService {
        -String secretKey
        -long jwtExpiration
        -long refreshExpiration
        +extractUsername(String) String
        +generateToken(UserDetails) String
        +generateRefreshToken(UserDetails) String
        +isTokenValid(String,UserDetails) boolean
        +isTokenExpired(String) boolean
        -extractClaim(String,Function) T
        -buildToken(Map,UserDetails,long) String
    }

    %% DTOs
    class CreateJobRequest {
        -String name
        -JobType type
        -JobPriority priority
        -JobScheduleDto schedule
        -JobPayloadDto payload
        -RetryPolicyDto retryPolicy
    }

    class JobResponse {
        -String id
        -String name
        -JobType type
        -JobStatus status
        -JobPriority priority
        -String ownerEmail
        -JobScheduleDto schedule
        -RetryPolicyDto retryPolicy
        -Instant createdAt
        -Instant updatedAt
        -JobRunSummaryDto latestRun
    }

    class ErrorResponse {
        -Instant timestamp
        -int status
        -String error
        -String message
        -String path
        -Map~String,String~ details
    }

    %% Relationships
    JobController --> JobService
    JobController --> JobMapper
    AuthController --> AuthenticationService
    NotificationController --> NotificationService
    DLQController --> DLQService
    AuditController --> AuditService
    
    JobMapper --> CreateJobRequest
    JobMapper --> JobResponse
    GlobalExceptionHandler --> ErrorResponse
```

## 🔧 Configuration & Infrastructure

```mermaid
classDiagram
    class QuartzConfig {
        -DataSource dataSource
        +schedulerFactoryBean() SchedulerFactoryBean
        +quartzProperties() Properties
        +jobFactory(ApplicationContext) SpringBeanJobFactory
    }

    class DatabaseConfig {
        +dataSource() DataSource
        +transactionManager() PlatformTransactionManager
        +entityManagerFactory() LocalContainerEntityManagerFactoryBean
        +databasePoolProperties() DatabasePoolProperties
    }

    class SecurityConfig {
        +securityFilterChain(HttpSecurity) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationProvider() AuthenticationProvider
        +corsConfigurationSource() CorsConfigurationSource
    }

    class MetricsConfig {
        +jobMetrics(MeterRegistry) JobMetrics
        +metricsAspect() MetricsAspect
        +customActuatorEndpoints() List~Endpoint~
    }

    class CacheConfiguration {
        +cacheManager() CacheManager
        +redisTemplate() RedisTemplate
        +cacheStatisticsCollector() CacheStatisticsCollector
    }

    class MailConfig {
        +mailSender() JavaMailSender
        +templateEngine() TemplateEngine
        +templateResolver() ITemplateResolver
    }

    class JobMetrics {
        -Counter jobSubmissions
        -Counter jobExecutions
        -Counter jobRetries
        -Gauge runningJobs
        -Timer executionTimer
        +recordJobSubmission() void
        +recordJobSuccess() void
        +recordJobFailure() void
        +recordJobRetry() void
        +recordJobExecution(Duration) void
        +incrementRunningJobs() void
        +decrementRunningJobs() void
    }

    class JobSchedulerHealthIndicator {
        -Scheduler scheduler
        -JobRepository jobRepository
        +health() Health
        -getSchedulerStatus() Map~String,Object~
        -getJobStatistics() Map~String,Object~
    }

    class JobStatisticsEndpoint {
        -JobRepository jobRepository
        -JobRunRepository jobRunRepository
        +jobStats() Map~String,Object~
        +getJobsByStatus() Map~JobStatus,Long~
        +getRecentExecutionStats() Map~String,Object~
    }

    %% Relationships
    QuartzConfig --> DatabaseConfig
    SecurityConfig --> JwtService
    MetricsConfig --> JobMetrics
    JobMetrics --> JobSchedulerHealthIndicator
    JobStatisticsEndpoint --> JobRepository
```
