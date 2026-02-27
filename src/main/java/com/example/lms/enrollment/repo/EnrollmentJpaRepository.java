package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, Long> {

    boolean existsByUserIdAndCourseSessionIdAndStatus(Long userId, Long courseSessionId, String status);

    EnrollmentEntity findTopByUserIdAndCourseSessionIdAndStatusOrderByIdDesc(Long userId, Long courseSessionId, String status);

    @Query(value = """
            SELECT c.course_code AS courseCode,
                   cs.section AS section,
                   c.title AS title,
                   c.professor AS professor,
                   cs.room AS room,
                   cs.day_of_week AS day,
                   TIME_FORMAT(cs.start_time, '%H:%i') AS startTime,
                   TIME_FORMAT(cs.end_time, '%H:%i') AS endTime
            FROM enrollments e
            JOIN course_sessions cs ON e.course_session_id = cs.id
            JOIN courses c ON cs.course_id = c.id
            WHERE e.user_id = :userId
              AND e.status = 'ENROLLED'
            ORDER BY FIELD(cs.day_of_week, '월','화','수','목','금'), cs.start_time
            """, nativeQuery = true)
    List<TimetableLectureProjection> findEnrolledLectures(@Param("userId") Long userId);

    @Query(value = """
            SELECT c.course_code AS courseCode,
                   cs.section AS section,
                   c.title AS title,
                   c.professor AS professor,
                   CONCAT(cs.day_of_week, ' ', TIME_FORMAT(cs.start_time, '%H:%i'), '-', TIME_FORMAT(cs.end_time, '%H:%i')) AS classTime,
                   CASE WHEN c.price = 0 THEN '무료' ELSE CONCAT('₩', FORMAT(c.price, 0)) END AS price,
                   cs.enrolled_count AS enrolledCount,
                   cs.max_count AS maxCount,
                   e.status AS status
            FROM enrollments e
            JOIN course_sessions cs ON e.course_session_id = cs.id
            JOIN courses c ON cs.course_id = c.id
            WHERE e.user_id = :userId
              AND e.status IN (:statuses)
            ORDER BY e.id DESC
            """, nativeQuery = true)
    List<MyPageCourseProjection> findMyCoursesByStatuses(@Param("userId") Long userId,
                                                          @Param("statuses") Set<String> statuses);
}

