USE lms_db;

CREATE TABLE IF NOT EXISTS lessons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  video_url VARCHAR(500) NOT NULL,
  order_no INT NOT NULL,
  thumbnail_url VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_lessons_course FOREIGN KEY (course_id) REFERENCES courses(id),
  INDEX idx_lessons_course_order (course_id, order_no)
);

CREATE TABLE IF NOT EXISTS lesson_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  lesson_id BIGINT NOT NULL,
  progress_percent INT NOT NULL DEFAULT 0,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_lesson_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_lesson_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id),
  CONSTRAINT uq_lesson_progress_user_lesson UNIQUE (user_id, lesson_id),
  INDEX idx_lesson_progress_user (user_id)
);
