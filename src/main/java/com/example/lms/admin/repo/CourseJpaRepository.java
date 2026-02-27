package com.example.lms.admin.repo;

import com.example.lms.admin.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {
    boolean existsByCourseCode(String courseCode);
    boolean existsBySubjectCode(String subjectCode);
}
