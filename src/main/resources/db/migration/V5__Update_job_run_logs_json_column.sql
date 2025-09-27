-- Update the context column to use jsonb type for better JSON handling
ALTER TABLE job_run_logs MODIFY COLUMN context JSON;
