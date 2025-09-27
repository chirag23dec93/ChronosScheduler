# Chronos Database Schema - Quick Reference

## Table Summary

| Table | Purpose | Key Relationships | Records |
|-------|---------|-------------------|---------|
| **USERS** | User management & auth | → JOBS, NOTIFICATIONS, AUDIT_EVENTS | ~100s |
| **JOBS** | Core job definitions | ← USERS, → SCHEDULES, PAYLOADS, RUNS | ~1000s |
| **JOB_SCHEDULES** | Timing configuration | ← JOBS (1:1) | ~1000s |
| **JOB_PAYLOADS** | Job configuration | ← JOBS (1:1) | ~1000s |
| **RETRY_POLICIES** | Failure handling | ← JOBS (1:0..1) | ~100s |
| **JOB_RUNS** | Execution history | ← JOBS, → LOGS | ~10000s |
| **JOB_RUN_LOGS** | Detailed logs | ← JOB_RUNS | ~100000s |
| **NOTIFICATIONS** | User alerts | ← JOBS, USERS | ~1000s |
| **DLQ_EVENTS** | Failed jobs | ← JOBS, JOB_RUNS | ~10s |
| **AUDIT_EVENTS** | System audit | ← USERS | ~10000s |
| **QRTZ_*** | Quartz scheduler | Internal Quartz | ~1000s |

## Core Entity Relationships

```
USER (1) ──────── (*) JOB
                      │
                      ├── (1) JOB_SCHEDULE
                      ├── (1) JOB_PAYLOAD  
                      ├── (0..1) RETRY_POLICY
                      ├── (*) JOB_RUN
                      │       └── (*) JOB_RUN_LOG
                      ├── (*) NOTIFICATION
                      └── (*) DLQ_EVENT
```

## Job Type Support Matrix

| Job Type | Payload Fields | Status | Features |
|----------|----------------|--------|----------|
| **HTTP** | url, method, headers, body | ✅ Production | REST API calls, auth, timeouts |
| **SCRIPT** | script content | ✅ Production | Shell/Python/etc execution |
| **DATABASE** | url, query, params | ✅ Production | SQL execution, transactions |
| **FILE_SYSTEM** | path, operation, target | ✅ Production | File ops, compression |
| **MESSAGE_QUEUE** | queue, operation, config | ✅ Production | Kafka/RabbitMQ integration |
| **DB_TO_KAFKA** | db_url, query, topic | ✅ Production | Real-time data streaming |
| **CACHE** | region, operation, keys | ✅ Production | Redis cache management |
| **REPORT** | type, recipients, format | ✅ Production | PDF/Excel generation |
| **DUMMY** | - | ✅ Testing | Test/placeholder jobs |

## Key Database Features

### 🔐 Security
- **Authentication**: Email/password with JWT
- **Authorization**: Role-based access control (USER, ADMIN)
- **Ownership**: Jobs owned by users, access control enforced
- **Audit Trail**: All actions logged with user attribution

### 🔄 Retry Mechanism
- **Configurable Policies**: Per-job retry configuration
- **Backoff Strategies**: Fixed and exponential backoff
- **Condition-Based**: Retry on specific error types
- **Attempt Tracking**: Full retry history in job runs

### 📊 Monitoring & Observability
- **Execution Tracking**: Complete job run history
- **Detailed Logging**: Structured logs with context
- **Performance Metrics**: Duration, success rates
- **Dead Letter Queue**: Failed job management

### ⚡ Performance
- **ULID Primary Keys**: Time-ordered, efficient
- **Strategic Indexes**: Optimized for common queries
- **Polymorphic Design**: Flexible job type support
- **Quartz Integration**: Clustered, persistent scheduling

### 🏗️ Architecture Patterns
- **Single Table Inheritance**: Job payloads with type discrimination
- **Event Sourcing**: Complete audit trail of all changes
- **CQRS-like**: Separate read/write optimizations
- **Domain-Driven Design**: Clear entity boundaries

## Sample Queries

### Most Common Operations
```sql
-- Get user's active jobs
SELECT j.*, js.schedule_type, js.next_fire_time 
FROM jobs j 
JOIN job_schedules js ON j.id = js.job_id 
WHERE j.owner_id = ? AND j.status IN ('SCHEDULED', 'RUNNING');

-- Job execution history with logs
SELECT jr.*, jrl.level, jrl.message 
FROM job_runs jr 
LEFT JOIN job_run_logs jrl ON jr.id = jrl.run_id 
WHERE jr.job_id = ? 
ORDER BY jr.scheduled_time DESC, jrl.timestamp;

-- Failed jobs needing attention
SELECT j.name, dlq.reason, dlq.created_at 
FROM dlq_events dlq 
JOIN jobs j ON dlq.job_id = j.id 
WHERE dlq.created_at > DATE_SUB(NOW(), INTERVAL 24 HOUR);
```

## Migration History

| Version | Description | Impact |
|---------|-------------|--------|
| V1 | Initial schema | Core tables created |
| V2 | Quartz tables | Scheduler persistence |
| V3-V7 | Column updates | Data type refinements |
| V8 | Payload types | Multi job type support |
| V9 | Type constraints | Enhanced validation |
| V10 | Dummy jobs | Testing support |
| V11 | Message queue | Kafka/RabbitMQ fields |
| V12 | DB_TO_KAFKA | Streaming job support |
| V13 | Type constraints | Updated validations |

## Production Considerations

### Scaling
- **Horizontal**: Quartz clustering for multiple instances
- **Vertical**: Indexed queries for large datasets
- **Partitioning**: Consider partitioning job_runs by date
- **Archival**: Implement log retention policies

### Monitoring
- **Key Metrics**: Job success rates, execution times, queue depths
- **Alerts**: Failed job thresholds, DLQ accumulation
- **Dashboards**: Real-time job execution monitoring
- **Health Checks**: Database connectivity, Quartz scheduler status

### Backup & Recovery
- **Full Backups**: Daily database snapshots
- **Incremental**: Transaction log backups
- **Point-in-Time**: Recovery to specific timestamps
- **Testing**: Regular restore testing procedures

This database schema supports enterprise-grade job scheduling with comprehensive features for reliability, monitoring, and scalability.
