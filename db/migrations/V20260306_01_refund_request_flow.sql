USE lms_db;

SET @has_refund_status := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'point_transactions' AND column_name = 'refund_status'
);
SET @sql := IF(@has_refund_status = 0,
  'ALTER TABLE point_transactions ADD COLUMN refund_status VARCHAR(30) NULL AFTER memo',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_refund_requested_at := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'point_transactions' AND column_name = 'refund_requested_at'
);
SET @sql := IF(@has_refund_requested_at = 0,
  'ALTER TABLE point_transactions ADD COLUMN refund_requested_at DATETIME NULL AFTER refund_status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_refund_processed_at := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'point_transactions' AND column_name = 'refund_processed_at'
);
SET @sql := IF(@has_refund_processed_at = 0,
  'ALTER TABLE point_transactions ADD COLUMN refund_processed_at DATETIME NULL AFTER refund_requested_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_refund_reject_reason := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'point_transactions' AND column_name = 'refund_reject_reason'
);
SET @sql := IF(@has_refund_reject_reason = 0,
  'ALTER TABLE point_transactions ADD COLUMN refund_reject_reason VARCHAR(255) NULL AFTER refund_processed_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE point_transactions
SET refund_status = CASE
  WHEN type = 'SPEND' THEN 'PAID'
  WHEN type = 'REFUND' THEN 'REFUND_APPROVED'
  ELSE refund_status
END
WHERE refund_status IS NULL;

CREATE INDEX idx_pt_refund_status ON point_transactions(refund_status);
CREATE INDEX idx_pt_refund_requested_at ON point_transactions(refund_requested_at);
