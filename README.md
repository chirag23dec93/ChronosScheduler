# Chronos - Distributed Job Scheduling System

Chronos is a production-grade distributed job scheduling system built with Spring Boot that enables reliable execution, management, and monitoring of one-time and recurring jobs.

## 🎯 Features

- **Job Scheduling**: Submit jobs to run immediately or at a future time
- **Recurring Jobs**: Support for hourly, daily, weekly, and monthly schedules
- **Job Management**: View status, cancel, pause/resume, and reschedule jobs via RESTful APIs
- **Failure Handling**: Automatic retries with configurable policies and backoff strategies
- **Observability**: Detailed execution logs, status tracking, and system health metrics
- **Security**: JWT-based authentication and role-based access control
- **Scalability**: Designed for horizontal scaling with clustered Quartz scheduler

## 🏗️ Architecture

```plantuml
@startuml
!theme plain
skinparam componentStyle rectangle

[REST API Layer] as api
[Service Layer] as service
[Scheduler Layer] as scheduler
[Worker/Executor] as worker
database "PostgreSQL" as db
queue "Notification\nService" as notify

api --> service : Submit/Control Jobs
service --> scheduler : Schedule/Trigger
scheduler --> worker : Execute
worker --> db : Record Status/Logs
worker --> notify : On completion/failure

note right of api : JWT Auth + RBAC
note right of scheduler : Quartz Cluster
note right of worker : Retries + DLQ
note right of db : ACID + Idempotency
@enduml
```

### Job Execution Flow

```plantuml
@startuml
!theme plain
actor User
participant "API" as api
participant "Service" as svc
participant "Scheduler" as sch
participant "Worker" as wrk
database "DB" as db
queue "Notify" as not

User -> api : Submit Job
api -> svc : Validate & Create
svc -> db : Persist Job + Schedule
svc -> sch : Schedule Trigger

sch -> wrk : Execute Job
wrk -> db : Update Status=RUNNING

alt Success
    wrk -> db : Status=SUCCEEDED
    wrk -> not : Success Notification
else Failure (Retryable)
    wrk -> db : Status=FAILED
    wrk -> sch : Schedule Retry
    sch -> wrk : Retry Execution
else Max Retries Exceeded
    wrk -> db : Status=FAILED
    wrk -> db : Create DLQ Entry
    wrk -> not : Failure Notification
end

User -> api : Get Status
api -> db : Query Status
db -> User : Job Status + Logs
@enduml
```

## 🚀 Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.8+

### Local Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/chronos.git
   cd chronos
   ```

2. Start infrastructure services:
   ```bash
   make up
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run -Dspring.profiles.active=local
   ```

4. Access the API documentation:
   - OpenAPI UI: http://localhost:8080/swagger-ui.html
   - API Docs: http://localhost:8080/v3/api-docs

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/chronos` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `chronos` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `chronos` |
| `JWT_SECRET_KEY` | JWT signing key (Base64) | Generated in local profile |
| `QUARTZ_CLUSTERED` | Enable Quartz clustering | `false` in local |

## 📡 API Overview

### Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret"}'

# Login and get JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret"}'
```

### Job Management

```bash
# Create a one-time job
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sample HTTP Job",
    "type": "HTTP",
    "schedule": {
      "type": "ONCE",
      "runAt": "2023-12-25T10:00:00Z"
    },
    "payload": {
      "httpUrl": "https://httpbin.org/get",
      "httpMethod": "GET"
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "backoffStrategy": "EXPONENTIAL",
      "backoffSeconds": 60
    }
  }'

# Create a recurring job
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Daily Report",
    "type": "HTTP",
    "schedule": {
      "type": "CRON",
      "cronExpr": "0 0 12 * * ?"
    },
    "payload": {
      "httpUrl": "https://api.internal/generate-report",
      "httpMethod": "POST"
    }
  }'
```

## 📊 Monitoring

### Metrics

Available at `/actuator/prometheus`:

- `chronos_jobs_total{status}`
- `chronos_runs_duration_seconds`
- `chronos_retries_total`
- `chronos_queue_depth`

### Health Checks

Available at `/actuator/health`:

- Database connectivity
- Quartz scheduler status
- Disk space
- Application status

## 🧪 Testing

```bash
# Run all tests (unit + integration)
./mvnw verify

# Run only unit tests
./mvnw test

# Run integration tests
./mvnw verify -P integration-test
```

## 📦 Deployment

### Docker

```bash
# Build image
docker build -t chronos .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/chronos \
  chronos
```

### Production Checklist

- [ ] Configure secure JWT secret
- [ ] Enable Quartz clustering
- [ ] Set up monitoring (Prometheus + Grafana)
- [ ] Configure email/webhook notifications
- [ ] Set appropriate retry policies
- [ ] Enable rate limiting
- [ ] Configure CORS for web UI

## 📚 Documentation

- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Database Schema](docs/schema.md)
- [Security Guide](docs/security.md)
- [Operations Guide](docs/ops.md)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
