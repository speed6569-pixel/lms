package com.example.lms.admin.repo;

import com.example.lms.admin.entity.AttendanceRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordJpaRepository extends JpaRepository<AttendanceRecordEntity, Long> {
}
