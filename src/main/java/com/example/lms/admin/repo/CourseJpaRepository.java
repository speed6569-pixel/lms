package com.example.lms.admin.repo;

import com.example.lms.admin.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, Long> {
    boolean existsByCourseCode(String courseCode);
    boolean existsBySubjectCode(String subjectCode);
    boolean existsByCourseCodeAndIdNot(String courseCode, Long id);
    boolean existsBySubjectCodeAndIdNot(String subjectCode, Long id);

    List<CourseEntity> findTop4ByStatusInAndActiveIsNotAndIsDeletedFalseOrderByCreatedAtDesc(List<String> statuses, Boolean active);
    List<CourseEntity> findByIsDeletedFalseOrderByIdDesc();
}
