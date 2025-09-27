# Chronos Job Scheduling System - Extended Class Diagram

## Monitoring & Metrics

```mermaid
classDiagram
    %% Monitoring Classes
    class JobMetrics {
        -MeterRegistry registry
        -Counter jobSubmissionCounter
        -Counter jobSuccessCounter
        -Counter jobFailureCounter
        -Counter jobRetryCounter
        -AtomicInteger runningJobsGauge
        -Timer jobExecutionTimer
        -DistributionSummary jobPayloadSize
        +init() void
        +recordJobSubmission() void
        +recordJobSuccess() void
        +recordJobFailure() void
        +recordJobRetry() void
        +setRunningJobs(int) void
        +startJobExecution() Timer.Sample
        +stopJobExecution(Timer.Sample) void
        +recordPayloadSize(long) void
    }

    class MetricsAspect {
        -AtomicInteger runningJobs
        -Counter jobSubmissionsTotal
        -Counter jobRetriesTotal
        -MeterRegistry registry
        +timeMethod(ProceedingJoinPoint) Object
        +trackJobCreation(ProceedingJoinPoint) Object
        +trackJobTrigger(ProceedingJoinPoint) Object
        +trackJobExecution(ProceedingJoinPoint) Object
        +trackJobRetry(ProceedingJoinPoint) Object
        +trackJobStatus(Job, JobStatus) void
    }

    class JobStatisticsEndpoint {
        -JobRepository jobRepository
        -JobRunRepository jobRunRepository
        +jobStatistics() Map~String,Object~
        -getJobsByStatus() Map~JobStatus,Long~
        -getRecentExecutionStats() Map~String,Object~
        -getAverageDurations() Map~String,Double~
        -getSuccessFailureRates() Map~String,Double~
    }

    class JobSchedulerHealthIndicator {
        -QuartzSchedulerService schedulerService
        -JobRepository jobRepository
        +health() Health
        -checkSchedulerStatus() boolean
        -getRunningJobsCount() long
        -getSchedulerMetadata() Map~String,Object~
    }

    class ClusterInfoEndpoint {
        -QuartzSchedulerService schedulerService
        -Environment environment
        +clusterInfo() Map~String,Object~
        -getInstanceInfo() Map~String,Object~
        -getClusterStatus() Map~String,Object~
        -getSystemResources() Map~String,Object~
    }

    %% Relationships
    JobMetrics --> MeterRegistry
    MetricsAspect --> MeterRegistry
    JobStatisticsEndpoint --> JobRepository
    JobStatisticsEndpoint --> JobRunRepository
    JobSchedulerHealthIndicator --> QuartzSchedulerService
    ClusterInfoEndpoint --> QuartzSchedulerService
```

## Security & Authentication

```mermaid
classDiagram
    %% Security Classes
    class SecurityConfig {
        -JwtAuthenticationFilter jwtAuthFilter
        -AuthenticationProvider authenticationProvider
        +securityFilterChain(HttpSecurity) SecurityFilterChain
        +authenticationManager(AuthenticationConfiguration) AuthenticationManager
        +corsConfigurationSource() CorsConfigurationSource
        -configureHttpSecurity(HttpSecurity) void
        -configureCors(HttpSecurity) void
        -configureSessionManagement(HttpSecurity) void
    }

    class JwtService {
        -String secretKey
        -long jwtExpiration
        -long refreshExpiration
        +extractUsername(String) String
        +generateToken(UserDetails) String
        +generateRefreshToken(UserDetails) String
        +isTokenValid(String, UserDetails) boolean
        +isTokenExpired(String) boolean
        -extractClaim(String, Function) T
        -extractAllClaims(String) Claims
        -buildToken(Map, UserDetails, long) String
        -getSignInKey() Key
    }

    class JwtAuthenticationFilter {
        -JwtService jwtService
        -UserDetailsService userDetailsService
        +doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain) void
        -extractTokenFromRequest(HttpServletRequest) String
        -authenticateUser(String, HttpServletRequest) void
    }

    class AuthenticationService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtService jwtService
        -AuthenticationManager authenticationManager
        +register(RegisterRequest) AuthResponse
        +authenticate(LoginRequest) AuthResponse
        +refreshToken(RefreshTokenRequest) AuthResponse
        -buildAuthResponse(User) AuthResponse
    }

    class User {
        -Long id
        -String email
        -String passwordHash
        -String roles
        -Instant createdAt
        +getAuthorities() Collection~GrantedAuthority~
        +getPassword() String
        +getUsername() String
        +isAccountNonExpired() boolean
        +isAccountNonLocked() boolean
        +isCredentialsNonExpired() boolean
        +isEnabled() boolean
    }

    %% Spring Security Interfaces
    class UserDetails {
        <<interface>>
        +getAuthorities() Collection~GrantedAuthority~
        +getPassword() String
        +getUsername() String
        +isAccountNonExpired() boolean
        +isAccountNonLocked() boolean
        +isCredentialsNonExpired() boolean
        +isEnabled() boolean
    }

    class UserDetailsService {
        <<interface>>
        +loadUserByUsername(String) UserDetails
    }

    %% Relationships
    SecurityConfig --> JwtAuthenticationFilter
    JwtAuthenticationFilter --> JwtService
    JwtAuthenticationFilter --> UserDetailsService
    AuthenticationService --> JwtService
    AuthenticationService --> UserRepository
    User ..|> UserDetails
    AuthenticationService ..|> UserDetailsService
```

## Configuration Classes

```mermaid
classDiagram
    %% Configuration Classes
    class QuartzConfig {
        -DataSource dataSource
        +schedulerFactoryBean() SchedulerFactoryBean
        +quartzProperties() Properties
        +jobFactory(ApplicationContext) SpringBeanJobFactory
        -configureClusterProperties(Properties) void
        -configurePersistenceProperties(Properties) void
        -configureThreadPoolProperties(Properties) void
    }

    class DatabaseConfig {
        +dataSource() DataSource
        +transactionManager(EntityManagerFactory) PlatformTransactionManager
        +entityManagerFactory(DataSource) LocalContainerEntityManagerFactoryBean
        +jpaProperties() Properties
        -configureHikariCP(HikariConfig) void
        -configureJpaProperties(Properties) void
    }

    class CacheConfiguration {
        +cacheManager() CacheManager
        +redisTemplate() RedisTemplate~String,Object~
        +redisConnectionFactory() LettuceConnectionFactory
        +cacheResolver() CacheResolver
        -configureCacheProperties() RedisCacheConfiguration
    }

    class RestTemplateConfig {
        +restTemplate() RestTemplate
        +clientHttpRequestFactory() ClientHttpRequestFactory
        -configureTimeouts(ClientHttpRequestFactory) void
        -configureConnectionPooling(ClientHttpRequestFactory) void
    }

    class MailConfig {
        +javaMailSender() JavaMailSender
        +templateEngine() TemplateEngine
        +templateResolver() ClassLoaderTemplateResolver
        +messageSource() MessageSource
        -configureMailProperties(JavaMailSenderImpl) void
        -configureTemplateEngine(SpringTemplateEngine) void
    }

    class MetricsConfig {
        +meterRegistry() MeterRegistry
        +prometheusRegistry() PrometheusMeterRegistry
        +timedAspect(MeterRegistry) TimedAspect
        +countedAspect(MeterRegistry) CountedAspect
        -configureCommonTags(MeterRegistry) void
    }

    class WebConfig {
        +corsConfigurationSource() CorsConfigurationSource
        +messageConverters() List~HttpMessageConverter~
        +objectMapper() ObjectMapper
        +webMvcConfigurer() WebMvcConfigurer
        -configureCors(CorsConfiguration) void
        -configureObjectMapper(ObjectMapper) void
    }

    class OpenApiConfig {
        +openAPI() OpenAPI
        +groupedOpenApi() GroupedOpenApi
        -apiInfo() Info
        -securityScheme() SecurityScheme
        -securityRequirement() SecurityRequirement
    }

    %% Factory Classes
    class DatabaseJobDataSourceFactory {
        -Map~String,DataSource~ dataSourceCache
        +getDataSource(String) DataSource
        +createDataSource(String) DataSource
        +closeDataSource(String) void
        -parseConnectionString(String) Properties
        -configureDataSource(HikariConfig, Properties) void
    }

    class SpringBeanJobFactory {
        -ApplicationContext applicationContext
        +createJobInstance(TriggerFiredBundle) Object
        -autowireBean(Object) void
    }

    %% Relationships
    QuartzConfig --> SpringBeanJobFactory
    DatabaseConfig --> DataSource
    CacheConfiguration --> CacheManager
    RestTemplateConfig --> RestTemplate
    MailConfig --> JavaMailSender
    MetricsConfig --> MeterRegistry
    WebConfig --> ObjectMapper
    OpenApiConfig --> OpenAPI
    DatabaseJobDataSourceFactory --> DataSource
```

## Exception Handling

```mermaid
classDiagram
    %% Exception Classes
    class ChronosException {
        <<abstract>>
        -String message
        -Throwable cause
        +ChronosException(String)
        +ChronosException(String, Throwable)
        +getMessage() String
        +getCause() Throwable
    }

    class JobExecutionException {
        -String jobId
        -String runId
        +JobExecutionException(String)
        +JobExecutionException(String, Throwable)
        +getJobId() String
        +getRunId() String
    }

    class InvalidJobConfigurationException {
        -String field
        -Object value
        +InvalidJobConfigurationException(String)
        +InvalidJobConfigurationException(String, String, Object)
        +getField() String
        +getValue() Object
    }

    class ResourceNotFoundException {
        -String resourceType
        -String resourceId
        +ResourceNotFoundException(String, String)
        +forResource(String, String) ResourceNotFoundException
        +getResourceType() String
        +getResourceId() String
    }

    class GlobalExceptionHandler {
        +handleJobExecutionException(JobExecutionException) ResponseEntity~ProblemDetail~
        +handleInvalidJobConfigurationException(InvalidJobConfigurationException) ResponseEntity~ProblemDetail~
        +handleResourceNotFoundException(ResourceNotFoundException) ResponseEntity~ProblemDetail~
        +handleValidationException(MethodArgumentNotValidException) ResponseEntity~ProblemDetail~
        +handleAccessDeniedException(AccessDeniedException) ResponseEntity~ProblemDetail~
        +handleGenericException(Exception) ResponseEntity~ProblemDetail~
        -createProblemDetail(HttpStatus, String, String) ProblemDetail
        -extractValidationErrors(BindingResult) Map~String,String~
    }

    %% Relationships
    ChronosException <|-- JobExecutionException
    ChronosException <|-- InvalidJobConfigurationException
    ChronosException <|-- ResourceNotFoundException
    RuntimeException <|-- ChronosException
```

## Event System

```mermaid
classDiagram
    %% Event Classes
    class JobLogEvent {
        -String jobId
        -String runId
        -String level
        -String message
        -Map~String,Object~ context
        -Instant timestamp
        +JobLogEvent(String, String, String, String)
        +JobLogEvent(String, String, String, String, Map)
        +getJobId() String
        +getRunId() String
        +getLevel() String
        +getMessage() String
        +getContext() Map~String,Object~
        +getTimestamp() Instant
    }

    class JobFailureEvent {
        -Job job
        -JobRun run
        -String reason
        -Instant timestamp
        +JobFailureEvent(Object, Job, JobRun, String)
        +getJob() Job
        +getRun() JobRun
        +getReason() String
        +getTimestamp() Instant
    }

    class JobFailureHandler {
        -DLQService dlqService
        -NotificationService notificationService
        +handleJobFailure(JobFailureEvent) void
        -shouldAddToDLQ(Job, JobRun) boolean
        -sendFailureNotification(Job, JobRun, String) void
    }

    %% Spring Event Classes
    class ApplicationEvent {
        <<abstract>>
        -Object source
        -long timestamp
        +ApplicationEvent(Object)
        +getSource() Object
        +getTimestamp() long
    }

    class ApplicationEventPublisher {
        <<interface>>
        +publishEvent(Object) void
        +publishEvent(ApplicationEvent) void
    }

    class EventListener {
        <<annotation>>
    }

    %% Relationships
    ApplicationEvent <|-- JobLogEvent
    ApplicationEvent <|-- JobFailureEvent
    JobFailureHandler --> DLQService
    JobFailureHandler --> NotificationService
```

## Validation System

```mermaid
classDiagram
    %% Validation Classes
    class JobPayloadValidator {
        <<interface>>
        +validate(JobPayload) void
        +supports(JobType) boolean
    }

    class DatabaseJobPayloadValidator {
        +validate(JobPayload) void
        +supports(JobType) boolean
        -validateQuery(String) void
        -validateDatabaseUrl(String) void
        -validateParameters(Map) void
    }

    class CronExpressionValidator {
        +isValid(String, ConstraintValidatorContext) boolean
        -validateCronExpression(String) boolean
        -parseCronExpression(String) CronExpression
    }

    class JobPayloadValidatorRegistry {
        -List~JobPayloadValidator~ validators
        +getValidator(JobType) JobPayloadValidator
        +registerValidator(JobPayloadValidator) void
        +validatePayload(JobPayload) void
    }

    %% Bean Validation
    class ValidCronExpression {
        <<annotation>>
        +message() String
        +groups() Class[]
        +payload() Class[]
    }

    class Valid {
        <<annotation>>
    }

    class NotNull {
        <<annotation>>
    }

    class NotBlank {
        <<annotation>>
    }

    %% Relationships
    JobPayloadValidator <|.. DatabaseJobPayloadValidator
    JobPayloadValidatorRegistry --> JobPayloadValidator
    CronExpressionValidator --> ValidCronExpression
```

This extended class diagram complements the main class diagram by showing:

1. **Monitoring & Metrics**: JobMetrics, MetricsAspect, health indicators, and statistics endpoints
2. **Security & Authentication**: JWT-based security, authentication filters, and user management
3. **Configuration**: Spring configuration classes for various components
4. **Exception Handling**: Custom exceptions and global error handling
5. **Event System**: Application events for job lifecycle management
6. **Validation**: Custom validators and validation annotations

Together, these diagrams provide a complete view of the Chronos job scheduling system's architecture, showing how all components interact to provide a robust, secure, and observable job scheduling platform.
