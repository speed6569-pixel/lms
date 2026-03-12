USE lms_db;

CREATE TABLE IF NOT EXISTS job_groups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(60) NOT NULL UNIQUE,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_levels (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_group_id BIGINT NOT NULL,
  name VARCHAR(60) NOT NULL,
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_job_levels_group FOREIGN KEY (job_group_id) REFERENCES job_groups(id),
  CONSTRAINT uq_job_levels_group_name UNIQUE (job_group_id, name)
);

INSERT INTO job_groups(name, active)
VALUES ('개발',1),('인사',1),('총무',1),('생산',1),('영업',1)
ON DUPLICATE KEY UPDATE active = VALUES(active);

-- 기본 직급을 모든 직군에 연결
INSERT INTO job_levels(job_group_id, name, active)
SELECT g.id, lv.name, 1
FROM job_groups g
JOIN (
  SELECT '사원' AS name UNION ALL SELECT '대리' UNION ALL SELECT '과장' UNION ALL SELECT '차장' UNION ALL SELECT '부장'
) lv
LEFT JOIN job_levels jl ON jl.job_group_id = g.id AND jl.name = lv.name
WHERE jl.id IS NULL;
