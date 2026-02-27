USE lms_db;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30) NULL AFTER email;

CREATE TABLE IF NOT EXISTS login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_login_history_user_time (user_id, login_time)
);
