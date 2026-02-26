package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, Long> {

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
}
