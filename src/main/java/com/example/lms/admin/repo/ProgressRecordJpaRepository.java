package com.example.lms.admin.repo;

import com.example.lms.admin.entity.ProgressRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRecordJpaRepository extends JpaRepository<ProgressRecordEntity, Long> {
}
