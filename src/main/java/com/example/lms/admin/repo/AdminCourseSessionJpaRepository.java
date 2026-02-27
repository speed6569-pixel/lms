package com.example.lms.admin.repo;

import com.example.lms.admin.entity.AdminCourseSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminCourseSessionJpaRepository extends JpaRepository<AdminCourseSessionEntity, Long> {
    List<AdminCourseSessionEntity> findByCourse_Id(Long courseId);
}
