USE lms_db;

SET @has_selected_days := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'selected_days'
);
SET @sql := IF(@has_selected_days = 0,
  'ALTER TABLE courses ADD COLUMN selected_days VARCHAR(50) NULL AFTER class_time',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
