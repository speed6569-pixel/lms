package com.example.lms.learn.repo;

import com.example.lms.learn.entity.LessonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonJpaRepository extends JpaRepository<LessonEntity, Long> {
    List<LessonEntity> findByCourseIdOrderByOrderNoAsc(Long courseId);
}
