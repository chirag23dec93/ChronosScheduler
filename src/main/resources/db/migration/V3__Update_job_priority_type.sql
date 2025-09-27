-- First update existing values to their string equivalents
UPDATE jobs SET priority = 
    CASE priority
        WHEN 0 THEN 'LOW'
        WHEN 1 THEN 'MEDIUM'
        ELSE 'HIGH'
    END;

-- Then modify the column type
ALTER TABLE jobs MODIFY priority VARCHAR(255) NOT NULL;
