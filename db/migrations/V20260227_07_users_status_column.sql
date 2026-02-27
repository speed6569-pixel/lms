USE lms_db;

SET @has_status := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'status'
);
SET @sql := IF(@has_status = 0,
  'ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''ACTIVE''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE users
SET status = CASE WHEN enabled = 1 THEN 'ACTIVE' ELSE 'BLOCKED' END
WHERE status IS NULL OR status = '';
