USE lms_db;

SET @has_course_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='enrollments' AND column_name='course_id'
);
SET @sql := IF(@has_course_id=0, 'ALTER TABLE enrollments ADD COLUMN course_id BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_applied_at := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name='enrollments' AND column_name='applied_at'
);
SET @sql := IF(@has_applied_at=0, 'ALTER TABLE enrollments ADD COLUMN applied_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- backfill course_id from course_session_id
UPDATE enrollments e
JOIN course_sessions cs ON cs.id = e.course_session_id
SET e.course_id = cs.course_id
WHERE e.course_id IS NULL;

SET @has_uq := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'enrollments' AND index_name = 'uq_enroll_user_course'
);
SET @sql := IF(@has_uq=0, 'ALTER TABLE enrollments ADD UNIQUE KEY uq_enroll_user_course (user_id, course_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
