USE lms_db;

SET @has_is_deleted := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='courses' AND column_name='is_deleted'
);
SET @sql := IF(@has_is_deleted = 0,
  'ALTER TABLE courses ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE INDEX idx_courses_is_deleted ON courses(is_deleted);
