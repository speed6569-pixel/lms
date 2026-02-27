USE lms_db;

SET @has_max_count := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'max_count'
);
SET @sql := IF(@has_max_count = 0,
  'ALTER TABLE courses ADD COLUMN max_count INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_class_time := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'courses' AND column_name = 'class_time'
);
SET @sql := IF(@has_class_time = 0,
  'ALTER TABLE courses ADD COLUMN class_time VARCHAR(255) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
