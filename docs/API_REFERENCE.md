# 📚 Chronos API Reference

## 🔐 Authentication

### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "roles": ["USER"]
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "email": "user@example.com",
  "roles": ["USER"]
}
```

## 💼 Job Management

### Create Job
```http
POST /api/jobs
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Daily Report Generation",
  "type": "HTTP",
  "priority": "HIGH",
  "schedule": {
    "scheduleType": "CRON",
    "cronExpression": "0 0 9 * * MON-FRI",
    "timezone": "America/New_York",
    "misfirePolicy": "FIRE_NOW"
  },
  "payload": {
    "type": "HTTP",
    "httpUrl": "https://api.example.com/generate-report",
    "httpMethod": "POST",
    "httpHeaders": {
      "Content-Type": "application/json",
      "Authorization": "Bearer api-token"
    },
    "httpBody": "{\"reportType\": \"daily\"}"
  },
  "retryPolicy": {
    "maxAttempts": 3,
    "backoffStrategy": "EXPONENTIAL",
    "backoffSeconds": 60,
    "retryOn": ["5XX", "TIMEOUT", "CONNECTION_ERROR"]
  }
}
```

### Get Job Details
```http
GET /api/jobs/{jobId}
Authorization: Bearer {token}
```

### List Jobs
```http
GET /api/jobs?page=0&size=20&type=HTTP&status=SCHEDULED
Authorization: Bearer {token}
```

### Job Lifecycle Operations
```http
# Pause Job
POST /api/jobs/{jobId}:pause
Authorization: Bearer {token}

# Resume Job
POST /api/jobs/{jobId}:resume
Authorization: Bearer {token}

# Cancel Job
POST /api/jobs/{jobId}:cancel
Authorization: Bearer {token}

# Delete Job
DELETE /api/jobs/{jobId}
Authorization: Bearer {token}
```

## 📊 Job Execution & Monitoring

### Get Job Runs
```http
GET /api/jobs/{jobId}/runs?page=0&size=20
Authorization: Bearer {token}
```

### Get Job Run Logs
```http
GET /api/jobs/{jobId}/runs/{runId}/logs
Authorization: Bearer {token}
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "runId": "01HGW2...",
      "timestamp": "2024-01-01T09:00:00Z",
      "level": "INFO",
      "message": "Starting HTTP request to https://api.example.com",
      "context": {
        "url": "https://api.example.com/generate-report",
        "method": "POST"
      }
    }
  ],
  "totalElements": 25,
  "totalPages": 3
}
```

## 🔔 Notifications

### Create Notification
```http
POST /api/notifications
Authorization: Bearer {token}
Content-Type: application/json

{
  "jobId": "01HGW2...",
  "channel": "EMAIL",
  "target": "admin@example.com",
  "templateCode": "JOB_SUCCESS",
  "payload": {
    "jobName": "Daily Report Generation",
    "completionTime": "2024-01-01T09:05:00Z"
  }
}
```

### List Notifications
```http
GET /api/notifications?userId={userId}&status=PENDING
Authorization: Bearer {token}
```

## 🚨 Dead Letter Queue (DLQ)

### List Failed Jobs
```http
GET /api/dlq?page=0&size=20
Authorization: Bearer {token}
```

### Replay Failed Job
```http
POST /api/dlq/{eventId}:replay
Authorization: Bearer {token}
```

### Resolve DLQ Event
```http
POST /api/dlq/{eventId}:resolve
Authorization: Bearer {token}
Content-Type: application/json

{
  "reason": "Issue resolved, job configuration updated"
}
```

## 📋 Audit Logs

### Get Audit Events
```http
GET /api/audit?page=0&size=20&entityType=Job&action=CREATE
Authorization: Bearer {token}
```

### Get Entity History
```http
GET /api/audit/entity/{entityType}/{entityId}
Authorization: Bearer {token}
```

## 🎯 Job Types & Payloads

### HTTP Job
```json
{
  "type": "HTTP",
  "httpUrl": "https://api.example.com/webhook",
  "httpMethod": "POST",
  "httpHeaders": {
    "Content-Type": "application/json",
    "Authorization": "Bearer token"
  },
  "httpBody": "{\"data\": \"value\"}",
  "timeoutSeconds": 30
}
```

### Database Job
```json
{
  "type": "DATABASE",
  "databaseUrl": "jdbc:mysql://localhost:3306/mydb?user=user&password=pass",
  "query": "SELECT COUNT(*) as user_count FROM users WHERE active = ?",
  "parameters": {
    "active": true
  },
  "queryTimeoutSeconds": 60,
  "readOnly": true
}
```

### File System Job
```json
{
  "type": "FILE_SYSTEM",
  "operation": "COPY",
  "path": "/source/file.txt",
  "targetPath": "/backup/file.txt",
  "createDirectories": true,
  "overwrite": false
}
```

### Message Queue Job
```json
{
  "type": "MESSAGE_QUEUE",
  "queueName": "user-events",
  "operationType": "PRODUCE",
  "messageBody": "{\"userId\": 123, \"event\": \"login\"}",
  "messageGroupId": "user-123",
  "queueConfig": {
    "messageKey": "user-123",
    "headers": {
      "source": "chronos-scheduler"
    }
  }
}
```

### Cache Job
```json
{
  "type": "CACHE",
  "operation": "WARM",
  "region": "user-cache",
  "keys": ["user:123", "user:456"],
  "timeToLiveSeconds": 3600,
  "skipIfExists": true
}
```

### DB to Kafka Job
```json
{
  "type": "DB_TO_KAFKA",
  "databaseUrl": "jdbc:mysql://localhost:3306/analytics",
  "query": "SELECT * FROM events WHERE created_at > ?",
  "kafkaTopic": "analytics-events",
  "kafkaKeyField": "event_id",
  "batchSize": 1000,
  "includeMetadata": true,
  "fieldMappings": {
    "created_at": "timestamp"
  }
}
```

## 📅 Schedule Types

### One-time Execution
```json
{
  "scheduleType": "ONCE",
  "runAt": "2024-01-01T12:00:00Z",
  "misfirePolicy": "FIRE_NOW"
}
```

### CRON Expression
```json
{
  "scheduleType": "CRON",
  "cronExpression": "0 0 9 * * MON-FRI",
  "timezone": "America/New_York",
  "misfirePolicy": "RESCHEDULE"
}
```

### Interval-based
```json
{
  "scheduleType": "INTERVAL",
  "intervalSeconds": 3600,
  "misfirePolicy": "IGNORE"
}
```

## 🔄 Retry Policies

### Exponential Backoff
```json
{
  "maxAttempts": 5,
  "backoffStrategy": "EXPONENTIAL",
  "backoffSeconds": 30,
  "retryOn": ["5XX", "TIMEOUT", "CONNECTION_ERROR"]
}
```

### Fixed Backoff
```json
{
  "maxAttempts": 3,
  "backoffStrategy": "FIXED",
  "backoffSeconds": 60,
  "retryOn": ["TIMEOUT"]
}
```

## 📊 Monitoring Endpoints

### Health Check
```http
GET /actuator/health
```

### Job Statistics
```http
GET /actuator/job-stats
Authorization: Bearer {token}
```

### Cluster Information
```http
GET /actuator/cluster-info
Authorization: Bearer {token}
```

### Prometheus Metrics
```http
GET /actuator/prometheus
```

## 🚫 Error Responses

### Standard Error Format
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/jobs",
  "details": {
    "name": "Job name is required",
    "schedule.cronExpression": "Invalid CRON expression"
  }
}
```

### Common HTTP Status Codes
- **200**: Success
- **201**: Created
- **400**: Bad Request (validation errors)
- **401**: Unauthorized (missing/invalid token)
- **403**: Forbidden (insufficient permissions)
- **404**: Not Found
- **409**: Conflict (duplicate resource)
- **500**: Internal Server Error

## 🔍 Query Parameters

### Pagination
- `page`: Page number (0-based)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

### Filtering
- `type`: Job type filter
- `status`: Job status filter
- `priority`: Job priority filter
- `owner`: Job owner filter
- `createdAfter`: Created after timestamp
- `createdBefore`: Created before timestamp

### Example
```http
GET /api/jobs?page=0&size=10&type=HTTP&status=SCHEDULED&sort=createdAt,desc
```
