-- Core LMS schema aligned with current Spring entities/repositories
-- Target: MySQL 8+

CREATE DATABASE IF NOT EXISTS lms_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE lms_db;

-- 1) users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(120) NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(80) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2) courses
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    professor VARCHAR(80) NULL,
    job_group VARCHAR(50) NULL,
    job_level VARCHAR(50) NULL,
    price INT NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_courses_job (job_group, job_level),
    INDEX idx_courses_active (active)
);

-- 3) course_sessions
CREATE TABLE IF NOT EXISTS course_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    section VARCHAR(20) NOT NULL,
    day_of_week VARCHAR(10) NOT NULL, -- 월/화/수/목/금/토/일
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(60) NULL,
    day_night VARCHAR(20) NULL,       -- 주간/야간
    enrolled_count INT NOT NULL DEFAULT 0,
    max_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_sessions_course FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT uq_course_sessions UNIQUE (course_id, section),
    INDEX idx_course_sessions_time (day_of_week, start_time, end_time),
    INDEX idx_course_sessions_status (status)
);

-- 4) enrollments
CREATE TABLE IF NOT EXISTS enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_session_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,      -- REQUESTED/ENROLLED/CANCELED/REJECTED/COMPLETED
    enrolled_at DATETIME NULL,
    canceled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_enrollments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_enrollments_session FOREIGN KEY (course_session_id) REFERENCES course_sessions(id),
    INDEX idx_enrollments_user_status (user_id, status),
    INDEX idx_enrollments_session_status (course_session_id, status)
);

-- 5) attendance_records
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,      -- PRESENT/LATE/ABSENT/EXCUSED
    minutes_attended INT NULL,
    minutes_total INT NULL,
    recorded_by VARCHAR(80) NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    INDEX idx_attendance_enrollment (enrollment_id),
    INDEX idx_attendance_session_date (session_date)
);

-- 6) progress_records
CREATE TABLE IF NOT EXISTS progress_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    unit_type VARCHAR(30) NULL,       -- VIDEO/QUIZ/ASSIGNMENT...
    unit_id VARCHAR(80) NULL,
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    studied_seconds INT NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    INDEX idx_progress_enrollment (enrollment_id),
    INDEX idx_progress_recorded_at (recorded_at)
);
