USE lms_db;

SET @has_writer_login_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'support_posts' AND column_name = 'writer_login_id'
);
SET @sql := IF(@has_writer_login_id = 0,
  'ALTER TABLE support_posts ADD COLUMN writer_login_id VARCHAR(100) NULL AFTER writer',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_writer_user_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'support_posts' AND column_name = 'writer_user_id'
);
SET @sql := IF(@has_writer_user_id = 0,
  'ALTER TABLE support_posts ADD COLUMN writer_user_id BIGINT NULL AFTER writer_login_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE support_posts sp
LEFT JOIN users u ON u.login_id = sp.writer OR u.name = sp.writer
SET sp.writer_login_id = COALESCE(sp.writer_login_id, u.login_id, sp.writer),
    sp.writer_user_id = COALESCE(sp.writer_user_id, u.id)
WHERE sp.writer_login_id IS NULL OR sp.writer_login_id = '';
