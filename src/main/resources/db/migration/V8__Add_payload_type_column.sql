-- Add payload_type column and constraint
ALTER TABLE job_payloads
ADD COLUMN payload_type VARCHAR(50) NOT NULL DEFAULT 'HTTP',
ADD CONSTRAINT job_payloads_type_check CHECK (payload_type IN ('HTTP', 'SCRIPT', 'DUMMY', 'CACHE', 'FILE_SYSTEM', 'MESSAGE_QUEUE', 'DATABASE', 'REPORT'));

-- Add Cache job payload columns
ALTER TABLE job_payloads
ADD COLUMN region VARCHAR(255),
ADD COLUMN source_region VARCHAR(255),
ADD COLUMN time_to_live_seconds INTEGER,
ADD COLUMN skip_if_exists BOOLEAN,
ADD COLUMN async BOOLEAN,
ADD COLUMN cache_config JSON,
ADD COLUMN cache_keys JSON;

-- Add File system job payload columns
ALTER TABLE job_payloads
ADD COLUMN path TEXT,
ADD COLUMN operation VARCHAR(50),
ADD COLUMN content TEXT,
ADD COLUMN target_path TEXT,
ADD COLUMN create_directories BOOLEAN,
ADD COLUMN overwrite BOOLEAN,
ADD COLUMN parameters JSON;

-- Add Message queue job payload columns
ALTER TABLE job_payloads
ADD COLUMN queue_name VARCHAR(255),
ADD COLUMN message TEXT,
ADD COLUMN routing_key VARCHAR(255),
ADD COLUMN exchange VARCHAR(255),
ADD COLUMN priority INTEGER,
ADD COLUMN persistent BOOLEAN,
ADD COLUMN headers JSON;

-- Add Database job payload columns
ALTER TABLE job_payloads
ADD COLUMN database_url VARCHAR(255),
ADD COLUMN sql_query TEXT,
ADD COLUMN query_parameters JSON,
ADD COLUMN transaction_isolation VARCHAR(50),
ADD COLUMN query_timeout_seconds INTEGER,
ADD COLUMN max_rows INTEGER,
ADD COLUMN read_only BOOLEAN;

-- Add Report job payload columns
ALTER TABLE job_payloads
ADD COLUMN report_type VARCHAR(100),
ADD COLUMN recipients JSON,
ADD COLUMN format VARCHAR(50),
ADD COLUMN compress BOOLEAN,
ADD COLUMN template_name VARCHAR(255),
ADD COLUMN template_data JSON;
