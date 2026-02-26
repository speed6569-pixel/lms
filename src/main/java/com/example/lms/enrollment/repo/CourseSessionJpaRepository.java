package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.CourseSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseSessionJpaRepository extends JpaRepository<CourseSessionEntity, Long> {

    @Query(value = """
            SELECT c.course_code AS courseCode,
                   cs.section AS section,
                   c.job_group AS job,
                   c.job_level AS position,
                   c.title AS title,
                   c.professor AS professor,
                   CONCAT(cs.day_of_week, ' ', TIME_FORMAT(cs.start_time, '%H:%i'), '-', TIME_FORMAT(cs.end_time, '%H:%i')) AS classTime,
                   CASE WHEN c.price = 0 THEN '무료' ELSE CONCAT('₩', FORMAT(c.price, 0)) END AS price,
                   cs.enrolled_count AS enrolledCount,
                   cs.max_count AS maxCount,
                   cs.day_of_week AS day,
                   TIME_FORMAT(cs.start_time, '%H:%i') AS startTime,
                   TIME_FORMAT(cs.end_time, '%H:%i') AS endTime,
                   cs.day_night AS dayNight,
                   CASE WHEN cs.enrolled_count >= cs.max_count THEN '신청불가' ELSE '신청가능' END AS note
            FROM course_sessions cs
            JOIN courses c ON cs.course_id = c.id
            ORDER BY FIELD(cs.day_of_week, '월','화','수','목','금','토','일'), cs.start_time
            """, nativeQuery = true)
    List<CourseListProjection> findAllCourseRows();
}
