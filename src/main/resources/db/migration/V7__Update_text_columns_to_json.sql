-- Update job_payloads table
ALTER TABLE job_payloads 
    MODIFY COLUMN http_headers JSON,
    MODIFY COLUMN metadata JSON;

-- Update job_run_logs table
ALTER TABLE job_run_logs 
    MODIFY COLUMN context JSON;

-- Update notifications table
ALTER TABLE notifications 
    MODIFY COLUMN payload JSON;

-- Update audit_events table
ALTER TABLE audit_events 
    MODIFY COLUMN details JSON;
