-- Add DB_TO_KAFKA to the jobs_type_check constraint
ALTER TABLE jobs DROP CONSTRAINT jobs_type_check;
ALTER TABLE jobs ADD CONSTRAINT jobs_type_check 
CHECK (type IN ('HTTP', 'SCRIPT', 'DUMMY', 'DATABASE', 'FILE_SYSTEM', 'MESSAGE_QUEUE', 'EMAIL', 'CACHE', 'REPORT', 'DB_TO_KAFKA'));

-- Add DB_TO_KAFKA to the job_payloads_type_check constraint
ALTER TABLE job_payloads DROP CONSTRAINT job_payloads_type_check;
ALTER TABLE job_payloads ADD CONSTRAINT job_payloads_type_check 
CHECK (payload_type IN ('HTTP', 'SCRIPT', 'DUMMY', 'DATABASE', 'FILE_SYSTEM', 'MESSAGE_QUEUE', 'EMAIL', 'CACHE', 'REPORT', 'DB_TO_KAFKA'));
