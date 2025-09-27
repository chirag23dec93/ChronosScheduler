-- Add new fields for DB_TO_KAFKA job payload
ALTER TABLE job_payloads 
ADD COLUMN kafka_topic VARCHAR(255),
ADD COLUMN kafka_key_field VARCHAR(255),
ADD COLUMN kafka_headers JSON,
ADD COLUMN offset_field VARCHAR(255),
ADD COLUMN last_processed_value VARCHAR(255),
ADD COLUMN field_mappings JSON,
ADD COLUMN exclude_fields JSON,
ADD COLUMN include_metadata BOOLEAN DEFAULT TRUE,
ADD COLUMN dead_letter_topic VARCHAR(255),
ADD COLUMN skip_on_error BOOLEAN DEFAULT FALSE,
ADD COLUMN max_retries INT DEFAULT 3,
ADD COLUMN max_records INT,
ADD COLUMN connection_timeout_seconds INT DEFAULT 30;
