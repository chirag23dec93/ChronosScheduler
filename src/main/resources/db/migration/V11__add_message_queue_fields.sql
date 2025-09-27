-- Add new fields for MessageQueue job payload
ALTER TABLE job_payloads 
ADD COLUMN operation_type VARCHAR(50),
ADD COLUMN message_body TEXT,
ADD COLUMN message_group_id VARCHAR(255),
ADD COLUMN message_deduplication_id VARCHAR(255),
ADD COLUMN message_attributes JSON,
ADD COLUMN queue_config JSON,
ADD COLUMN batch_size INT,
ADD COLUMN visibility_timeout_seconds INT;

-- Update existing MESSAGE_QUEUE payloads to have default values
UPDATE job_payloads 
SET operation_type = 'PRODUCE',
    message_attributes = '{}',
    queue_config = '{"type": "KAFKA"}',
    batch_size = 10,
    visibility_timeout_seconds = 30
WHERE payload_type = 'MESSAGE_QUEUE' AND operation_type IS NULL;
