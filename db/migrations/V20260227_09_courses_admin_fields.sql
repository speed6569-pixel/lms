USE lms_db;

SET @has_subject_code := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='subject_code');
SET @sql := IF(@has_subject_code=0, 'ALTER TABLE courses ADD COLUMN subject_code VARCHAR(40) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_job_group := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='job_group');
SET @sql := IF(@has_job_group=0, 'ALTER TABLE courses ADD COLUMN job_group VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_job_level := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='job_level');
SET @sql := IF(@has_job_level=0, 'ALTER TABLE courses ADD COLUMN job_level VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_subject_name := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='subject_name');
SET @sql := IF(@has_subject_name=0, 'ALTER TABLE courses ADD COLUMN subject_name VARCHAR(150) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_instructor := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='instructor');
SET @sql := IF(@has_instructor=0, 'ALTER TABLE courses ADD COLUMN instructor VARCHAR(80) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_capacity := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='capacity');
SET @sql := IF(@has_capacity=0, 'ALTER TABLE courses ADD COLUMN capacity INT NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_status := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='status');
SET @sql := IF(@has_status=0, 'ALTER TABLE courses ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''OPEN''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
