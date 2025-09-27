-- Drop existing constraint
ALTER TABLE jobs DROP CONSTRAINT jobs_type_check;

-- Add updated constraint with all job types
ALTER TABLE jobs ADD CONSTRAINT jobs_type_check CHECK (type IN ('HTTP', 'SCRIPT', 'DUMMY', 'DATABASE', 'FILE_SYSTEM', 'MESSAGE_QUEUE', 'EMAIL', 'CACHE', 'REPORT'));
