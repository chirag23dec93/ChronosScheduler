================================================================================
                    CHRONOS JOB SCHEDULER - CURL EXAMPLES INDEX
================================================================================

This directory contains comprehensive curl request examples for all supported
job types in the Chronos job scheduling system. Each file includes multiple
scenarios covering success cases, failure cases, retry policies, and various
scheduling options.

================================================================================
                              FILE INDEX
================================================================================

1. http-jobs.txt
   - HTTP job type examples (10 scenarios)
   - GET, POST, PUT, DELETE, PATCH requests
   - Custom headers, authentication, timeouts
   - Success and failure cases with retry policies
   - Recurring jobs with CRON and INTERVAL schedules

2. database-jobs.txt
   - DATABASE job type examples (12 scenarios)
   - MySQL, PostgreSQL, SQL Server, Oracle connections
   - Simple queries, parameterized queries, complex joins
   - Read-only and write operations
   - Maintenance scripts and health checks
   - Cross-database operations

3. message-queue-jobs.txt
   - MESSAGE_QUEUE job type examples (12 scenarios)
   - Kafka and RabbitMQ operations
   - PRODUCE, CONSUME, PURGE, MOVE_DLQ operations
   - High-volume processing and error handling
   - Multi-topic operations and fanout patterns
   - AWS SQS integration examples

4. db-to-kafka-jobs.txt
   - DB_TO_KAFKA job type examples (10 scenarios)
   - Real-time database to Kafka streaming
   - Incremental data processing with offset tracking
   - Cross-database streaming (MySQL, PostgreSQL, SQL Server, Oracle)
   - Field mapping, transformation, and filtering
   - Error handling with Dead Letter Queue support

5. cache-jobs.txt
   - CACHE job type examples (12 scenarios)
   - WARM, INVALIDATE, SYNC, STATS operations
   - Redis integration with TTL support
   - Multi-region synchronization
   - Performance monitoring and statistics

6. retry-jobs.txt
   - RETRY POLICY examples (10 scenarios)
   - Fixed and exponential backoff strategies
   - Selective retry conditions and error handling
   - Aggressive, conservative, and fail-fast approaches
   - Retry monitoring and best practices
   - DLQ integration and job replay functionality

7. script-jobs.txt
   - SCRIPT job type examples (12 scenarios)
   - Bash, Python, and other script execution
   - System monitoring and maintenance scripts
   - File processing and data cleanup
   - Performance benchmarking and testing
   - Docker and network connectivity scripts

8. file-system-jobs.txt
   - FILE_SYSTEM job type examples (12 scenarios)
   - COPY, MOVE, DELETE, PROCESS, COMPRESS operations
   - File synchronization and backup
   - Batch processing and data transformation
   - Security operations (encryption, validation)
   - Large file handling and monitoring

9. dummy-jobs.txt
   - DUMMY job type examples (15 scenarios)
   - Testing and development scenarios
   - Performance and load testing
   - Error simulation and chaos engineering
   - Resource usage simulation
   - Concurrent execution testing

================================================================================
                           AUTHENTICATION
================================================================================

All examples use the same JWT token for authentication:
TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyZXRyeUB0ZXN0LmNvbSIsImlhdCI6MTc1ODM3MjU1NCwiZXhwIjoxNzU4NDU4OTU0fQ.Me91m3fwqVZ7eGHYBhScCCXAoj6o6BtCM0BZaxRnSVY"

To get a fresh token, use the authentication endpoint:
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "retry@test.com", "password": "password"}'

================================================================================
                           COMMON PATTERNS
================================================================================

Schedule Types:
- ONCE: Single execution at specified time
- CRON: Recurring based on cron expression (e.g., "0 */5 * * * ?")
- INTERVAL: Recurring at fixed intervals (e.g., 300 seconds)

Priority Levels:
- LOW: Background processing
- MEDIUM: Normal priority (default)
- HIGH: Urgent processing
- CRITICAL: Highest priority

Retry Strategies:
- FIXED: Fixed delay between retries
- EXPONENTIAL: Exponentially increasing delay

Common Retry Error Types:
- CONNECTION_ERROR: Network/connection issues
- TIMEOUT: Operation timeout
- DATABASE_ERROR: Database operation failures
- SCRIPT_ERROR: Script execution errors
- FILE_ERROR: File system operation errors
- CACHE_ERROR: Cache operation failures
- NETWORK_ERROR: Network connectivity issues

================================================================================
                           USAGE EXAMPLES
================================================================================

Basic Job Creation:
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "My Test Job",
    "type": "HTTP",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "ONCE",
      "runAt": "2025-09-20T18:00:00Z",
      "misfirePolicy": "FIRE_NOW"
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://httpbin.org/get",
      "httpMethod": "GET"
    }
  }'

Job with Retry Policy:
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Job with Retries",
    "type": "HTTP",
    "priority": "HIGH",
    "schedule": {
      "scheduleType": "ONCE",
      "runAt": "2025-09-20T18:00:00Z",
      "misfirePolicy": "FIRE_NOW"
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffStrategy": "EXPONENTIAL",
      "backoffSeconds": 10,
      "retryOn": ["5XX", "TIMEOUT", "CONNECTION_ERROR"]
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://httpbin.org/status/500",
      "httpMethod": "GET"
    }
  }'

Recurring Job (CRON):
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Hourly Health Check",
    "type": "HTTP",
    "priority": "LOW",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 0 * * * ?",
      "timezone": "UTC",
      "misfirePolicy": "FIRE_NOW"
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://httpbin.org/status/200",
      "httpMethod": "GET"
    }
  }'

================================================================================
                           JOB MANAGEMENT
================================================================================

Get Job Status:
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}

List All Jobs:
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/jobs?size=20&page=0"

Cancel Job:
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}:cancel

Pause Job:
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}:pause

Resume Job:
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}:resume

Get Job Runs:
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}/runs

Get Job Run Logs:
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}/runs/{runId}/logs

================================================================================
                           MONITORING
================================================================================

Health Check:
curl http://localhost:8080/actuator/health

Job Statistics:
curl http://localhost:8080/actuator/job-statistics

Cluster Information:
curl http://localhost:8080/actuator/cluster-info

Prometheus Metrics:
curl http://localhost:8080/actuator/prometheus

================================================================================
                           TIPS AND TRICKS
================================================================================

1. Testing Jobs:
   - Use DUMMY job type for testing without side effects
   - Start with ONCE schedule before using recurring schedules
   - Test retry policies with jobs that are expected to fail

2. Debugging:
   - Check job run logs for detailed execution information
   - Monitor job status transitions
   - Use health endpoints to verify system status

3. Performance:
   - Use appropriate priority levels for job importance
   - Monitor system resources during high-frequency jobs
   - Implement proper retry policies to handle transient failures

4. Security:
   - Always use valid JWT tokens for authentication
   - Protect sensitive data in job payloads
   - Use read-only database connections when possible

5. Scheduling:
   - Use UTC timezone for consistent scheduling
   - Test CRON expressions before deploying
   - Consider misfire policies for critical jobs

================================================================================
                           TROUBLESHOOTING
================================================================================

Common Issues:

1. Authentication Errors (401/403):
   - Verify JWT token is valid and not expired
   - Check user permissions for job operations
   - Ensure proper Authorization header format

2. Job Creation Failures (400):
   - Validate JSON payload structure
   - Check required fields are present
   - Verify enum values (priority, schedule type, etc.)

3. Job Execution Failures:
   - Check job run logs for error details
   - Verify external service availability
   - Review retry policy configuration

4. Scheduling Issues:
   - Validate CRON expressions
   - Check timezone settings
   - Verify system clock synchronization

5. Performance Issues:
   - Monitor system resources
   - Check database connection pool
   - Review job frequency and concurrency

================================================================================

For more detailed information about specific job types and their configuration
options, refer to the individual job type files in this directory.

Total Examples: 95+ curl requests covering all job types and scenarios
Last Updated: 2025-09-20

================================================================================
