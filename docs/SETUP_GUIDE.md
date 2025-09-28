# 🚀 Chronos Setup & Installation Guide

## 📋 Prerequisites

### System Requirements
- **Java**: 21 or higher
- **Maven**: 3.8+ 
- **Docker**: 20.10+ (for containerized setup)
- **Docker Compose**: 2.0+

### Database Requirements
- **MySQL**: 8.0+ (recommended for production)
- **PostgreSQL**: 13+ (alternative option)

### Optional Components
- **Redis**: 6.2+ (for caching)
- **Kafka**: 3.0+ (for message queues)
- **Grafana**: 9.0+ (for monitoring dashboards)

## 🔧 Installation Methods

### Method 1: Docker Compose (Recommended)

1. **Clone the repository**
```bash
git clone https://github.com/your-org/chronos-scheduler.git
cd chronos-scheduler
```

2. **Start all services**
```bash
docker-compose up -d
```

3. **Verify installation**
```bash
curl http://localhost:8080/actuator/health
```

### Method 2: Local Development Setup

1. **Clone and build**
```bash
git clone https://github.com/your-org/chronos-scheduler.git
cd chronos-scheduler
mvn clean install
```

2. **Setup database**
```bash
# MySQL
docker run -d --name chronos-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=chronos_db \
  -e MYSQL_USER=chronos_user \
  -e MYSQL_PASSWORD=chronos_pass \
  -p 3306:3306 mysql:8.0
```

3. **Configure application**
```bash
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml
# Edit database connection details
```

4. **Run the application**
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

## 🔑 Initial Configuration

### 1. Create Admin User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@chronos.com",
    "password": "SecurePassword123!",
    "roles": ["ADMIN"]
  }'
```

### 2. Get Authentication Token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@chronos.com",
    "password": "SecurePassword123!"
  }'
```

### 3. Test Job Creation
```bash
TOKEN="your-jwt-token-here"

curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Job",
    "type": "HTTP",
    "priority": "MEDIUM",
    "schedule": {
      "scheduleType": "ONCE",
      "runAt": "2024-01-01T12:00:00Z"
    },
    "payload": {
      "type": "HTTP",
      "httpUrl": "https://httpbin.org/get",
      "httpMethod": "GET"
    }
  }'
```

## 🐳 Docker Configuration

### Environment Variables
```yaml
# docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - DB_HOST=mysql
  - DB_PORT=3306
  - DB_NAME=chronos_db
  - DB_USER=chronos_user
  - DB_PASSWORD=chronos_pass
  - REDIS_HOST=redis
  - REDIS_PORT=6379
  - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

### Persistent Volumes
```yaml
volumes:
  - chronos_mysql_data:/var/lib/mysql
  - chronos_logs:/app/logs
  - chronos_config:/app/config
```

## 🔧 Configuration Profiles

### Available Profiles
- **local**: Local development with H2 database
- **docker**: Containerized environment
- **test**: Testing with TestContainers
- **prod**: Production configuration

### Profile-Specific Settings
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
chronos:
  scheduler:
    cluster-name: "chronos-prod-cluster"
    instance-id: "${HOSTNAME}"
```

## 📊 Monitoring Setup

### Prometheus Configuration
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'chronos'
    static_configs:
      - targets: ['chronos:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana Dashboard Import
1. Access Grafana: http://localhost:3000
2. Login: admin/admin
3. Import dashboard: `grafana/chronos-dashboard.json`

## 🔍 Health Checks

### Application Health
```bash
curl http://localhost:8080/actuator/health
```

### Database Health
```bash
curl http://localhost:8080/actuator/health/db
```

### Scheduler Health
```bash
curl http://localhost:8080/actuator/health/scheduler
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Check database status
docker logs chronos-mysql

# Verify connection
mysql -h localhost -P 3306 -u chronos_user -p chronos_db
```

#### 2. Scheduler Not Starting
```bash
# Check Quartz tables
SELECT * FROM QRTZ_SCHEDULER_STATE;

# Verify cluster configuration
curl http://localhost:8080/actuator/info
```

#### 3. Jobs Not Executing
```bash
# Check job status
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs

# View execution logs
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/jobs/{jobId}/runs
```

### Log Locations
- **Application Logs**: `/app/logs/chronos.log`
- **Database Logs**: `/var/lib/mysql/error.log`
- **Docker Logs**: `docker logs chronos-app`

## 🔧 Advanced Configuration

### Custom Job Executors
```java
@Component
public class CustomJobExecutor implements JobTypeExecutor {
    @Override
    public JobType getSupportedType() {
        return JobType.CUSTOM;
    }
    
    @Override
    public void execute(Job job, JobRun run) {
        // Custom execution logic
    }
}
```

### Security Configuration
```yaml
chronos:
  security:
    jwt:
      secret: "${JWT_SECRET}"
      expiration: 3600000  # 1 hour
    cors:
      allowed-origins: 
        - "http://localhost:3000"
        - "https://chronos.yourdomain.com"
```

## 📚 Next Steps

1. **Read the API Documentation**: http://localhost:8080/swagger-ui.html
2. **Explore CURL Examples**: See `curl-examples/` directory
3. **Setup Monitoring**: Configure Grafana dashboards
4. **Production Deployment**: Review production checklist
5. **Custom Job Types**: Implement custom executors
