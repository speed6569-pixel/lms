USE lms_db;

SET @has_point_balance := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'point_balance'
);
SET @sql := IF(@has_point_balance = 0,
  'ALTER TABLE users ADD COLUMN point_balance INT NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS point_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  course_id BIGINT NULL,
  type VARCHAR(20) NOT NULL,
  amount INT NOT NULL,
  balance_after INT NOT NULL,
  memo VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_point_transactions_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_point_transactions_course FOREIGN KEY (course_id) REFERENCES courses(id),
  INDEX idx_pt_user_created (user_id, created_at),
  INDEX idx_pt_course (course_id)
);
