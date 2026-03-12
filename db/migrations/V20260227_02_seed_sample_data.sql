-- Sample seed data for local/dev testing
USE lms_db;

-- users (sample seed accounts for local/dev only; replace in your own environment)
INSERT INTO users (login_id, email, password_hash, name, role, enabled)
VALUES
  ('seed_admin', 'seed_admin@example.local', '$2b$12$3xWdLJw9EwRpfH7D2WvO8eZrN2SR8i6Be6mMvxySN47n6MdX4N0yG', '샘플관리자', 'ROLE_ADMIN', 1),
  ('seed_user',  'seed_user@example.local',  '$2b$12$3xWdLJw9EwRpfH7D2WvO8eZrN2SR8i6Be6mMvxySN47n6MdX4N0yG', '샘플사용자', 'ROLE_USER', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  role = VALUES(role),
  enabled = VALUES(enabled);

-- courses
INSERT INTO courses (course_code, title, description, professor, job_group, job_level, price, active)
VALUES
  ('JAVA-101', '자바 기초 완성', '문법부터 객체지향까지', '김교수', '개발', '사원', 80000, 1),
  ('SPRING-201', '스프링부트 실전', 'API/DB/배포 실전', '이교수', '개발', '대리', 120000, 1),
  ('SQL-301', 'SQL 마스터 클래스', '실무 쿼리/성능 튜닝', '박교수', '개발', '주임', 95000, 1)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  professor = VALUES(professor),
  price = VALUES(price),
  active = VALUES(active);

-- course sessions
INSERT INTO course_sessions (course_id, section, day_of_week, start_time, end_time, room, day_night, enrolled_count, max_count, status)
SELECT c.id, '01', '월', '10:00:00', '12:00:00', 'A-301', '주간', 12, 30, 'OPEN' FROM courses c WHERE c.course_code='JAVA-101'
ON DUPLICATE KEY UPDATE room=VALUES(room), day_of_week=VALUES(day_of_week), start_time=VALUES(start_time), end_time=VALUES(end_time);

INSERT INTO course_sessions (course_id, section, day_of_week, start_time, end_time, room, day_night, enrolled_count, max_count, status)
SELECT c.id, '01', '화', '13:00:00', '15:00:00', 'B-201', '주간', 18, 30, 'OPEN' FROM courses c WHERE c.course_code='SPRING-201'
ON DUPLICATE KEY UPDATE room=VALUES(room), day_of_week=VALUES(day_of_week), start_time=VALUES(start_time), end_time=VALUES(end_time);

INSERT INTO course_sessions (course_id, section, day_of_week, start_time, end_time, room, day_night, enrolled_count, max_count, status)
SELECT c.id, '01', '수', '19:00:00', '21:00:00', 'C-102', '야간', 9, 25, 'OPEN' FROM courses c WHERE c.course_code='SQL-301'
ON DUPLICATE KEY UPDATE room=VALUES(room), day_of_week=VALUES(day_of_week), start_time=VALUES(start_time), end_time=VALUES(end_time);

-- enrollments for seed_user
INSERT INTO enrollments (user_id, course_session_id, status, enrolled_at, created_at)
SELECT u.id, cs.id, 'ENROLLED', NOW(), NOW()
FROM users u
JOIN courses c ON c.course_code='JAVA-101'
JOIN course_sessions cs ON cs.course_id=c.id AND cs.section='01'
WHERE u.login_id='seed_user'
  AND NOT EXISTS (
    SELECT 1 FROM enrollments e WHERE e.user_id=u.id AND e.course_session_id=cs.id AND e.status='ENROLLED'
  );

INSERT INTO enrollments (user_id, course_session_id, status, created_at)
SELECT u.id, cs.id, 'REQUESTED', NOW()
FROM users u
JOIN courses c ON c.course_code='SPRING-201'
JOIN course_sessions cs ON cs.course_id=c.id AND cs.section='01'
WHERE u.login_id='seed_user'
  AND NOT EXISTS (
    SELECT 1 FROM enrollments e WHERE e.user_id=u.id AND e.course_session_id=cs.id AND e.status='REQUESTED'
  );

-- attendance records for ENROLLED lecture
INSERT INTO attendance_records (enrollment_id, session_date, status, minutes_attended, minutes_total, recorded_by)
SELECT e.id, DATE_SUB(CURDATE(), INTERVAL 14 DAY), 'PRESENT', 120, 120, 'seed_admin'
FROM enrollments e
JOIN users u ON u.id=e.user_id
JOIN course_sessions cs ON cs.id=e.course_session_id
JOIN courses c ON c.id=cs.course_id
WHERE u.login_id='seed_user' AND c.course_code='JAVA-101' AND e.status='ENROLLED'
  AND NOT EXISTS (
    SELECT 1 FROM attendance_records ar WHERE ar.enrollment_id=e.id AND ar.session_date=DATE_SUB(CURDATE(), INTERVAL 14 DAY)
  );

INSERT INTO attendance_records (enrollment_id, session_date, status, minutes_attended, minutes_total, recorded_by)
SELECT e.id, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'LATE', 80, 120, 'seed_admin'
FROM enrollments e
JOIN users u ON u.id=e.user_id
JOIN course_sessions cs ON cs.id=e.course_session_id
JOIN courses c ON c.id=cs.course_id
WHERE u.login_id='seed_user' AND c.course_code='JAVA-101' AND e.status='ENROLLED'
  AND NOT EXISTS (
    SELECT 1 FROM attendance_records ar WHERE ar.enrollment_id=e.id AND ar.session_date=DATE_SUB(CURDATE(), INTERVAL 7 DAY)
  );

-- progress records for ENROLLED lecture
INSERT INTO progress_records (enrollment_id, unit_type, unit_id, progress_percent, completed, studied_seconds)
SELECT e.id, 'VIDEO', 'JAVA-101-UNIT-1', 100.00, 1, 3600
FROM enrollments e
JOIN users u ON u.id=e.user_id
JOIN course_sessions cs ON cs.id=e.course_session_id
JOIN courses c ON c.id=cs.course_id
WHERE u.login_id='seed_user' AND c.course_code='JAVA-101' AND e.status='ENROLLED'
  AND NOT EXISTS (
    SELECT 1 FROM progress_records pr WHERE pr.enrollment_id=e.id AND pr.unit_id='JAVA-101-UNIT-1'
  );

INSERT INTO progress_records (enrollment_id, unit_type, unit_id, progress_percent, completed, studied_seconds)
SELECT e.id, 'QUIZ', 'JAVA-101-QUIZ-1', 60.00, 0, 900
FROM enrollments e
JOIN users u ON u.id=e.user_id
JOIN course_sessions cs ON cs.id=e.course_session_id
JOIN courses c ON c.id=cs.course_id
WHERE u.login_id='seed_user' AND c.course_code='JAVA-101' AND e.status='ENROLLED'
  AND NOT EXISTS (
    SELECT 1 FROM progress_records pr WHERE pr.enrollment_id=e.id AND pr.unit_id='JAVA-101-QUIZ-1'
  );
