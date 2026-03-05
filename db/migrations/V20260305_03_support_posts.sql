USE lms_db;

CREATE TABLE IF NOT EXISTS support_posts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  writer VARCHAR(100) NOT NULL,
  answer TEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_support_posts_writer (writer),
  INDEX idx_support_posts_status (status),
  INDEX idx_support_posts_created (created_at)
);
