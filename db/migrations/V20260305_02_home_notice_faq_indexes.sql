USE lms_db;

CREATE INDEX idx_posts_home_notice_faq
ON posts (deleted, category, status, pinned, created_at);
