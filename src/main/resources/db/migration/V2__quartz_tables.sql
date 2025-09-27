-- Quartz Tables
-- Based on the standard Quartz MySQL schema

DROP TABLE IF EXISTS QRTZ_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPLE_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CRON_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SIMPROP_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_BLOB_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_CALENDARS;
DROP TABLE IF EXISTS QRTZ_PAUSED_TRIGGER_GRPS;
DROP TABLE IF EXISTS QRTZ_FIRED_TRIGGERS;
DROP TABLE IF EXISTS QRTZ_SCHEDULER_STATE;
DROP TABLE IF EXISTS QRTZ_LOCKS;
DROP TABLE IF EXISTS QRTZ_JOB_DETAILS;

CREATE TABLE QRTZ_JOB_DETAILS
(
    sched_name        VARCHAR(120) NOT NULL,
    job_name         VARCHAR(200) NOT NULL,
    job_group        VARCHAR(200) NOT NULL,
    description      VARCHAR(250) NULL,
    job_class_name   VARCHAR(250) NOT NULL,
    is_durable       BOOLEAN     NOT NULL,
    is_nonconcurrent BOOLEAN     NOT NULL,
    is_update_data   BOOLEAN     NOT NULL,
    requests_recovery BOOLEAN     NOT NULL,
    job_data         BLOB        NULL,
    PRIMARY KEY (sched_name, job_name, job_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_TRIGGERS
(
    sched_name     VARCHAR(120) NOT NULL,
    trigger_name   VARCHAR(200) NOT NULL,
    trigger_group  VARCHAR(200) NOT NULL,
    job_name      VARCHAR(200) NOT NULL,
    job_group     VARCHAR(200) NOT NULL,
    description   VARCHAR(250) NULL,
    next_fire_time BIGINT      NULL,
    prev_fire_time BIGINT      NULL,
    priority      INTEGER     NULL,
    trigger_state VARCHAR(16)  NOT NULL,
    trigger_type  VARCHAR(8)   NOT NULL,
    start_time    BIGINT      NOT NULL,
    end_time      BIGINT      NULL,
    calendar_name VARCHAR(200) NULL,
    misfire_instr SMALLINT    NULL,
    job_data     BLOB       NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, job_name, job_group)
        REFERENCES QRTZ_JOB_DETAILS (sched_name, job_name, job_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_SIMPLE_TRIGGERS
(
    sched_name      VARCHAR(120) NOT NULL,
    trigger_name    VARCHAR(200) NOT NULL,
    trigger_group   VARCHAR(200) NOT NULL,
    repeat_count    BIGINT      NOT NULL,
    repeat_interval BIGINT      NOT NULL,
    times_triggered BIGINT      NOT NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES QRTZ_TRIGGERS (sched_name, trigger_name, trigger_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_CRON_TRIGGERS
(
    sched_name      VARCHAR(120) NOT NULL,
    trigger_name    VARCHAR(200) NOT NULL,
    trigger_group   VARCHAR(200) NOT NULL,
    cron_expression VARCHAR(120) NOT NULL,
    time_zone_id    VARCHAR(80),
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES QRTZ_TRIGGERS (sched_name, trigger_name, trigger_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_SIMPROP_TRIGGERS
(
    sched_name    VARCHAR(120)   NOT NULL,
    trigger_name  VARCHAR(200)   NOT NULL,
    trigger_group VARCHAR(200)   NOT NULL,
    str_prop_1    VARCHAR(512)   NULL,
    str_prop_2    VARCHAR(512)   NULL,
    str_prop_3    VARCHAR(512)   NULL,
    int_prop_1    INT           NULL,
    int_prop_2    INT           NULL,
    long_prop_1   BIGINT        NULL,
    long_prop_2   BIGINT        NULL,
    dec_prop_1    NUMERIC(13, 4) NULL,
    dec_prop_2    NUMERIC(13, 4) NULL,
    bool_prop_1   BOOLEAN          NULL,
    bool_prop_2   BOOLEAN          NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES QRTZ_TRIGGERS (sched_name, trigger_name, trigger_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_BLOB_TRIGGERS
(
    sched_name    VARCHAR(120) NOT NULL,
    trigger_name  VARCHAR(200) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    blob_data    BLOB       NULL,
    PRIMARY KEY (sched_name, trigger_name, trigger_group),
    FOREIGN KEY (sched_name, trigger_name, trigger_group)
        REFERENCES QRTZ_TRIGGERS (sched_name, trigger_name, trigger_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_CALENDARS
(
    sched_name    VARCHAR(120) NOT NULL,
    calendar_name VARCHAR(200) NOT NULL,
    calendar     BLOB       NOT NULL,
    PRIMARY KEY (sched_name, calendar_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS
(
    sched_name    VARCHAR(120) NOT NULL,
    trigger_group VARCHAR(200) NOT NULL,
    PRIMARY KEY (sched_name, trigger_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_FIRED_TRIGGERS
(
    sched_name        VARCHAR(120) NOT NULL,
    entry_id         VARCHAR(95)  NOT NULL,
    trigger_name     VARCHAR(200) NOT NULL,
    trigger_group    VARCHAR(200) NOT NULL,
    instance_name    VARCHAR(200) NOT NULL,
    fired_time      BIGINT      NOT NULL,
    sched_time      BIGINT      NOT NULL,
    priority        INTEGER     NOT NULL,
    state           VARCHAR(16)  NOT NULL,
    job_name        VARCHAR(200) NULL,
    job_group       VARCHAR(200) NULL,
    is_nonconcurrent BOOLEAN        NULL,
    requests_recovery BOOLEAN        NULL,
    PRIMARY KEY (sched_name, entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_SCHEDULER_STATE
(
    sched_name        VARCHAR(120) NOT NULL,
    instance_name     VARCHAR(200) NOT NULL,
    last_checkin_time BIGINT      NOT NULL,
    checkin_interval  BIGINT      NOT NULL,
    PRIMARY KEY (sched_name, instance_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE QRTZ_LOCKS
(
    sched_name VARCHAR(120) NOT NULL,
    lock_name  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (sched_name, lock_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE INDEX idx_qrtz_j_req_recovery ON QRTZ_JOB_DETAILS (sched_name, requests_recovery);
CREATE INDEX idx_qrtz_j_grp ON QRTZ_JOB_DETAILS (sched_name, job_group);
CREATE INDEX idx_qrtz_t_j ON QRTZ_TRIGGERS (sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_t_jg ON QRTZ_TRIGGERS (sched_name, job_group);
CREATE INDEX idx_qrtz_t_c ON QRTZ_TRIGGERS (sched_name, calendar_name);
CREATE INDEX idx_qrtz_t_g ON QRTZ_TRIGGERS (sched_name, trigger_group);
CREATE INDEX idx_qrtz_t_state ON QRTZ_TRIGGERS (sched_name, trigger_state);
CREATE INDEX idx_qrtz_t_n_state ON QRTZ_TRIGGERS (sched_name, trigger_name, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_t_n_g_state ON QRTZ_TRIGGERS (sched_name, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_t_next_fire_time ON QRTZ_TRIGGERS (sched_name, next_fire_time);
CREATE INDEX idx_qrtz_t_nft_st ON QRTZ_TRIGGERS (sched_name, trigger_state, next_fire_time);
CREATE INDEX idx_qrtz_t_nft_misfire ON QRTZ_TRIGGERS (sched_name, misfire_instr, next_fire_time);
CREATE INDEX idx_qrtz_t_nft_st_misfire ON QRTZ_TRIGGERS (sched_name, misfire_instr, next_fire_time, trigger_state);
CREATE INDEX idx_qrtz_t_nft_st_misfire_grp ON QRTZ_TRIGGERS (sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);
CREATE INDEX idx_qrtz_ft_trig_inst_name ON QRTZ_FIRED_TRIGGERS (sched_name, instance_name);
CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON QRTZ_FIRED_TRIGGERS (sched_name, instance_name, requests_recovery);
CREATE INDEX idx_qrtz_ft_j_g ON QRTZ_FIRED_TRIGGERS (sched_name, job_name, job_group);
CREATE INDEX idx_qrtz_ft_jg ON QRTZ_FIRED_TRIGGERS (sched_name, job_group);
CREATE INDEX idx_qrtz_ft_t_g ON QRTZ_FIRED_TRIGGERS (sched_name, trigger_name, trigger_group);
CREATE INDEX idx_qrtz_ft_tg ON QRTZ_FIRED_TRIGGERS (sched_name, trigger_group);
