USE lms_db;

-- normalize legacy statuses to new policy statuses
UPDATE enrollments SET status = 'APPLIED'  WHERE status IN ('REQUESTED');
UPDATE enrollments SET status = 'APPROVED' WHERE status IN ('ENROLLED');
UPDATE enrollments SET status = 'CANCELLED' WHERE status IN ('CANCELED');

-- optional hardening: make sure cancel request spelling is consistent
UPDATE enrollments SET status = 'CANCEL_REQUESTED' WHERE status IN ('CANCEL-REQUESTED');
