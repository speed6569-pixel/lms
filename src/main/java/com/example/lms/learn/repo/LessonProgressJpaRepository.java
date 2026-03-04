package com.example.lms.learn.repo;

import com.example.lms.learn.entity.LessonProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonProgressJpaRepository extends JpaRepository<LessonProgressEntity, Long> {
    Optional<LessonProgressEntity> findByUserIdAndLessonId(Long userId, Long lessonId);
    List<LessonProgressEntity> findByUserIdAndLessonIdIn(Long userId, Collection<Long> lessonIds);

    @Query(value = """
            SELECT lp.*
            FROM lesson_progress lp
            JOIN lessons l ON l.id = lp.lesson_id
            WHERE lp.user_id = :userId AND l.course_id = :courseId
            ORDER BY lp.updated_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<LessonProgressEntity> findLatestByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
