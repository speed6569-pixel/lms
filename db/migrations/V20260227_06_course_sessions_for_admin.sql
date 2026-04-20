USE lms_db;

CREATE TABLE IF NOT EXISTS course_sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  section VARCHAR(20) NOT NULL,
  day_of_week VARCHAR(10) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  room VARCHAR(60) NULL,
  max_count INT NOT NULL DEFAULT 0,
  enrolled_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_course_sessions_course (course_id),
  INDEX idx_course_sessions_day_time (day_of_week, start_time, end_time)
);

SET @has_fk := (
  SELECT COUNT(*) FROM information_schema.key_column_usage
  WHERE table_schema = DATABASE() AND table_name = 'course_sessions' AND column_name='course_id' AND referenced_table_name='courses'
);
SET @sql := IF(@has_fk = 0,
  'ALTER TABLE course_sessions ADD CONSTRAINT fk_course_sessions_course FOREIGN KEY (course_id) REFERENCES courses(id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
