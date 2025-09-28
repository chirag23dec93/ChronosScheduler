# 📋 Chronos CURL Examples Guide

## 🔑 Authentication Setup

First, obtain an authentication token:

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "roles": ["USER"]
  }'

# Login to get token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!"
  }' | jq -r '.accessToken')

echo "Token: $TOKEN"
```

## 🌐 HTTP Jobs

### Basic HTTP GET Job
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Health Check API",
    "type": "HTTP",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "ONCE",
      "runAt": "2024-01-01T12:00:00Z"
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://httpbin.org/get",
      "httpMethod": "GET",
      "timeoutSeconds": 30
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffStrategy": "EXPONENTIAL",
      "backoffSeconds": 30,
      "retryOn": ["5XX", "TIMEOUT"]
    }
  }'
```

### HTTP POST with Authentication
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Webhook Notification",
    "type": "HTTP",
    "priority": "HIGH",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 0 9 * * MON-FRI",
      "timezone": "America/New_York"
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://api.slack.com/api/chat.postMessage",
      "httpMethod": "POST",
      "httpHeaders": {
        "Authorization": "Bearer xoxb-your-token",
        "Content-Type": "application/json"
      },
      "httpBody": "{\"channel\":\"#alerts\",\"text\":\"Daily report ready\"}",
      "timeoutSeconds": 60
    }
  }'
```

## 🗄️ Database Jobs

### Simple Database Query
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "User Count Report",
    "type": "DATABASE",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "INTERVAL",
      "intervalSeconds": 3600
    },
    "payload": {
      "type": "DATABASE",
      "databaseUrl": "jdbc:mysql://localhost:3306/myapp?user=dbuser&password=dbpass",
      "query": "SELECT COUNT(*) as user_count FROM users WHERE active = true",
      "queryTimeoutSeconds": 30,
      "readOnly": true
    }
  }'
```

### Parameterized Database Query
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Clean Old Records",
    "type": "DATABASE",
    "priority": "LOW",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 0 2 * * *"
    },
    "payload": {
      "type": "DATABASE",
      "databaseUrl": "jdbc:mysql://localhost:3306/myapp?user=admin&password=adminpass",
      "query": "DELETE FROM audit_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)",
      "parameters": {
        "1": 90
      },
      "queryTimeoutSeconds": 300,
      "readOnly": false
    },
    "retryPolicy": {
      "maxAttempts": 2,
      "backoffStrategy": "FIXED",
      "backoffSeconds": 300
    }
  }'
```

## 📁 File System Jobs

### File Copy Operation
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Backup Configuration Files",
    "type": "FILE_SYSTEM",
    "priority": "HIGH",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 0 1 * * *"
    },
    "payload": {
      "type": "FILE_SYSTEM",
      "operation": "COPY",
      "path": "/app/config/application.yml",
      "targetPath": "/backup/config/application-$(date +%Y%m%d).yml",
      "createDirectories": true,
      "overwrite": true
    }
  }'
```

### File Content Creation
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Generate Daily Report File",
    "type": "FILE_SYSTEM",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "ONCE",
      "runAt": "2024-01-01T23:59:00Z"
    },
    "payload": {
      "type": "FILE_SYSTEM",
      "operation": "WRITE",
      "path": "/reports/daily-$(date +%Y%m%d).txt",
      "content": "Daily Report Generated at $(date)",
      "createDirectories": true,
      "overwrite": false
    }
  }'
```

## 📨 Message Queue Jobs

### Kafka Message Production
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Send User Event to Kafka",
    "type": "MESSAGE_QUEUE",
    "priority": "HIGH",
    "schedule": {
      "scheduleType": "ONCE",
      "runAt": "2024-01-01T12:00:00Z"
    },
    "payload": {
      "type": "MESSAGE_QUEUE",
      "queueName": "user-events",
      "operationType": "PRODUCE",
      "messageBody": "{\"userId\":123,\"event\":\"login\",\"timestamp\":\"2024-01-01T12:00:00Z\"}",
      "messageGroupId": "user-123",
      "queueConfig": {
        "messageKey": "user-123",
        "headers": {
          "source": "chronos-scheduler",
          "version": "1.0"
        }
      }
    }
  }'
```

### Message Queue Consumption
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Process User Events",
    "type": "MESSAGE_QUEUE",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "INTERVAL",
      "intervalSeconds": 300
    },
    "payload": {
      "type": "MESSAGE_QUEUE",
      "queueName": "user-events",
      "operationType": "CONSUME",
      "batchSize": 10,
      "queueConfig": {
        "maxWaitTimeSeconds": 20,
        "visibilityTimeoutSeconds": 300
      }
    }
  }'
```

## 🗃️ Cache Jobs

### Cache Warming
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Warm User Cache",
    "type": "CACHE",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 0 6 * * *"
    },
    "payload": {
      "type": "CACHE",
      "operation": "WARM",
      "region": "user-cache",
      "keys": ["user:popular", "user:recent", "user:trending"],
      "timeToLiveSeconds": 3600,
      "skipIfExists": true
    }
  }'
```

### Cache Invalidation
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Clear Expired Cache",
    "type": "CACHE",
    "priority": "LOW",
    "schedule": {
      "scheduleType": "INTERVAL",
      "intervalSeconds": 7200
    },
    "payload": {
      "type": "CACHE",
      "operation": "INVALIDATE",
      "region": "session-cache",
      "keys": ["expired:*"]
    }
  }'
```

## 🔄 DB to Kafka Streaming Jobs

### Database to Kafka Stream
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Stream User Events to Kafka",
    "type": "DB_TO_KAFKA",
    "priority": "HIGH",
    "schedule": {
      "scheduleType": "INTERVAL",
      "intervalSeconds": 300
    },
    "payload": {
      "type": "DB_TO_KAFKA",
      "databaseUrl": "jdbc:mysql://localhost:3306/analytics?user=reader&password=pass",
      "query": "SELECT id, user_id, event_type, created_at FROM user_events WHERE created_at > ? ORDER BY created_at",
      "kafkaTopic": "user-analytics",
      "kafkaKeyField": "user_id",
      "batchSize": 1000,
      "includeMetadata": true,
      "skipOnError": true,
      "fieldMappings": {
        "created_at": "event_timestamp",
        "user_id": "userId"
      },
      "excludedFields": ["internal_id"]
    }
  }'
```

## 📊 Job Management Operations

### List All Jobs
```bash
curl -X GET "http://localhost:8080/api/jobs?page=0&size=20&type=HTTP&status=SCHEDULED" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Job Details
```bash
JOB_ID="01HGW2..."
curl -X GET "http://localhost:8080/api/jobs/$JOB_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Pause Job
```bash
curl -X POST "http://localhost:8080/api/jobs/$JOB_ID:pause" \
  -H "Authorization: Bearer $TOKEN"
```

### Resume Job
```bash
curl -X POST "http://localhost:8080/api/jobs/$JOB_ID:resume" \
  -H "Authorization: Bearer $TOKEN"
```

### Cancel Job
```bash
curl -X POST "http://localhost:8080/api/jobs/$JOB_ID:cancel" \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Job
```bash
curl -X DELETE "http://localhost:8080/api/jobs/$JOB_ID" \
  -H "Authorization: Bearer $TOKEN"
```

## 📈 Job Monitoring

### Get Job Execution History
```bash
curl -X GET "http://localhost:8080/api/jobs/$JOB_ID/runs?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Job Run Logs
```bash
RUN_ID="01HGW3..."
curl -X GET "http://localhost:8080/api/jobs/$JOB_ID/runs/$RUN_ID/logs" \
  -H "Authorization: Bearer $TOKEN"
```

### System Health Check
```bash
curl -X GET "http://localhost:8080/actuator/health" \
  -H "Authorization: Bearer $TOKEN"
```

### Job Statistics
```bash
curl -X GET "http://localhost:8080/actuator/job-stats" \
  -H "Authorization: Bearer $TOKEN"
```

### Prometheus Metrics
```bash
curl -X GET "http://localhost:8080/actuator/prometheus"
```

## 🔔 Notifications

### Create Email Notification
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "jobId": "'$JOB_ID'",
    "channel": "EMAIL",
    "target": "admin@example.com",
    "templateCode": "JOB_SUCCESS",
    "payload": {
      "jobName": "Daily Report Generation",
      "completionTime": "2024-01-01T09:05:00Z"
    }
  }'
```

### Create Webhook Notification
```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "jobId": "'$JOB_ID'",
    "channel": "WEBHOOK",
    "target": "https://hooks.slack.com/services/YOUR/WEBHOOK/URL",
    "templateCode": "JOB_FAILURE",
    "payload": {
      "jobName": "Critical Process",
      "errorMessage": "Database connection failed"
    }
  }'
```

## 🚨 Dead Letter Queue (DLQ)

### List Failed Jobs
```bash
curl -X GET "http://localhost:8080/api/dlq?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Replay Failed Job
```bash
DLQ_EVENT_ID="01HGW4..."
curl -X POST "http://localhost:8080/api/dlq/$DLQ_EVENT_ID:replay" \
  -H "Authorization: Bearer $TOKEN"
```

### Resolve DLQ Event
```bash
curl -X POST "http://localhost:8080/api/dlq/$DLQ_EVENT_ID:resolve" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "reason": "Configuration issue resolved, database connection restored"
  }'
```

## 📋 Audit Logs

### Get Audit Events
```bash
curl -X GET "http://localhost:8080/api/audit?page=0&size=20&entityType=Job&action=CREATE" \
  -H "Authorization: Bearer $TOKEN"
```

### Get Entity History
```bash
curl -X GET "http://localhost:8080/api/audit/entity/Job/$JOB_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Get User Activity
```bash
curl -X GET "http://localhost:8080/api/audit?userEmail=user@example.com&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## 🔧 Advanced Examples

### Complex HTTP Job with Retry Logic
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Complex API Integration",
    "type": "HTTP",
    "priority": "HIGH",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 */15 * * * *",
      "timezone": "UTC",
      "misfirePolicy": "FIRE_NOW"
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://api.external-service.com/data/sync",
      "httpMethod": "POST",
      "httpHeaders": {
        "Authorization": "Bearer api-token-here",
        "Content-Type": "application/json",
        "X-Client-ID": "chronos-scheduler",
        "X-Request-ID": "{{uuid}}"
      },
      "httpBody": "{\"syncType\":\"incremental\",\"lastSync\":\"{{lastRunTime}}\"}",
      "timeoutSeconds": 120
    },
    "retryPolicy": {
      "maxAttempts": 5,
      "backoffStrategy": "EXPONENTIAL",
      "backoffSeconds": 60,
      "retryOn": ["5XX", "TIMEOUT", "CONNECTION_ERROR", "4XX"]
    }
  }'
```

### Batch Processing Job
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Batch User Processing",
    "type": "DB_TO_KAFKA",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "CRON",
      "cronExpression": "0 0 */4 * * *"
    },
    "payload": {
      "type": "DB_TO_KAFKA",
      "databaseUrl": "jdbc:postgresql://localhost:5432/userdb?user=batch&password=batchpass",
      "query": "SELECT u.id, u.email, u.status, p.preferences FROM users u LEFT JOIN user_preferences p ON u.id = p.user_id WHERE u.updated_at > ? AND u.status = '\''active'\'' ORDER BY u.updated_at LIMIT 10000",
      "kafkaTopic": "user-batch-processing",
      "kafkaKeyField": "id",
      "batchSize": 500,
      "includeMetadata": true,
      "skipOnError": false,
      "fieldMappings": {
        "updated_at": "lastModified",
        "preferences": "userPrefs"
      }
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffStrategy": "FIXED",
      "backoffSeconds": 300
    }
  }'
```

## 💡 Tips & Best Practices

### Environment Variables
```bash
# Set up environment variables for reusability
export CHRONOS_URL="http://localhost:8080"
export CHRONOS_TOKEN="your-jwt-token-here"

# Use in requests
curl -X GET "$CHRONOS_URL/api/jobs" \
  -H "Authorization: Bearer $CHRONOS_TOKEN"
```

### Response Formatting
```bash
# Pretty print JSON responses
curl -X GET "$CHRONOS_URL/api/jobs" \
  -H "Authorization: Bearer $CHRONOS_TOKEN" | jq '.'

# Extract specific fields
curl -X GET "$CHRONOS_URL/api/jobs" \
  -H "Authorization: Bearer $CHRONOS_TOKEN" | jq '.content[].name'
```

### Error Handling
```bash
# Check HTTP status codes
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X GET "$CHRONOS_URL/api/jobs" \
  -H "Authorization: Bearer $CHRONOS_TOKEN")

if [ $HTTP_STATUS -eq 200 ]; then
  echo "Request successful"
else
  echo "Request failed with status: $HTTP_STATUS"
fi
```
