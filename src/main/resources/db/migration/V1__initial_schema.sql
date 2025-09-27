-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Jobs table
CREATE TABLE jobs (
    id VARCHAR(26) PRIMARY KEY,  -- ULID
    owner_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT jobs_name_owner_unique UNIQUE (name, owner_id),
    CONSTRAINT jobs_owner_fk FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT jobs_type_check CHECK (type IN ('HTTP', 'SCRIPT', 'DUMMY')),
    CONSTRAINT jobs_status_check CHECK (status IN ('PENDING', 'SCHEDULED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'PAUSED'))
) ENGINE=InnoDB;

CREATE INDEX idx_jobs_owner_status ON jobs(owner_id, status);
CREATE INDEX idx_jobs_status ON jobs(status);

-- Job Schedules table
CREATE TABLE job_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(26) NOT NULL,
    schedule_type VARCHAR(50) NOT NULL CHECK (schedule_type IN ('ONCE', 'CRON', 'INTERVAL')),
    run_at TIMESTAMP NULL,
    cron_expr VARCHAR(100),
    interval_seconds BIGINT,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    misfire_policy VARCHAR(50) NOT NULL,
    CONSTRAINT job_schedules_job_unique UNIQUE (job_id),
    CONSTRAINT job_schedules_job_fk FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT job_schedules_misfire_check CHECK (misfire_policy IN ('FIRE_NOW', 'IGNORE', 'RESCHEDULE'))
) ENGINE=InnoDB;

-- Job Payloads table
CREATE TABLE job_payloads (
    job_id VARCHAR(26) PRIMARY KEY,
    http_url TEXT,
    http_method VARCHAR(10),
    http_headers JSON,
    http_body TEXT,
    script TEXT,
    metadata JSON,
    CONSTRAINT job_payloads_job_fk FOREIGN KEY (job_id) REFERENCES jobs(id)
) ENGINE=InnoDB;

-- Retry Policies table
CREATE TABLE retry_policies (
    job_id VARCHAR(26) PRIMARY KEY,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    backoff_strategy VARCHAR(50) NOT NULL,
    backoff_seconds INTEGER NOT NULL DEFAULT 60,
    retry_on JSON NOT NULL DEFAULT ('[\'5xx\', \'timeout\', \'non2xx\', \'exception\']'),
    CONSTRAINT retry_policies_job_fk FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT retry_policies_strategy_check CHECK (backoff_strategy IN ('FIXED', 'EXPONENTIAL'))
) ENGINE=InnoDB;

-- Job Runs table
CREATE TABLE job_runs (
    id VARCHAR(26) PRIMARY KEY,  -- ULID
    job_id VARCHAR(26) NOT NULL,
    scheduled_time TIMESTAMP NOT NULL,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    attempt INTEGER NOT NULL DEFAULT 1,
    outcome VARCHAR(50),
    exit_code INTEGER,
    error_message TEXT,
    worker_id VARCHAR(255),
    duration_ms BIGINT,
    CONSTRAINT job_runs_job_fk FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT job_runs_outcome_check CHECK (outcome IN ('SUCCESS', 'FAILURE'))
) ENGINE=InnoDB;

CREATE INDEX idx_job_runs_job_id ON job_runs(job_id);
CREATE INDEX idx_job_runs_scheduled_time ON job_runs(scheduled_time);

-- Job Run Logs table
CREATE TABLE job_run_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(26) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    context JSON,
    CONSTRAINT job_run_logs_run_fk FOREIGN KEY (run_id) REFERENCES job_runs(id)
) ENGINE=InnoDB;

CREATE INDEX idx_job_run_logs_run_id ON job_run_logs(run_id);
CREATE INDEX idx_job_run_logs_timestamp ON job_run_logs(timestamp);

-- Notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(26) NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    target TEXT NOT NULL,
    template_code VARCHAR(100) NOT NULL,
    sent_at TIMESTAMP NULL,
    payload JSON,
    CONSTRAINT notifications_job_fk FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT notifications_user_fk FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT notifications_channel_check CHECK (channel IN ('EMAIL', 'WEBHOOK'))
) ENGINE=InnoDB;

CREATE INDEX idx_notifications_job_id ON notifications(job_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);

-- Dead Letter Queue table
CREATE TABLE dlq_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(26) NOT NULL,
    last_run_id VARCHAR(26) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dlq_events_job_fk FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT dlq_events_run_fk FOREIGN KEY (last_run_id) REFERENCES job_runs(id)
) ENGINE=InnoDB;

CREATE INDEX idx_dlq_events_job_id ON dlq_events(job_id);

-- Audit Events table
CREATE TABLE audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSON,
    CONSTRAINT audit_events_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX idx_audit_events_user_id ON audit_events(user_id);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
