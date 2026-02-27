package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.CourseSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;

public interface CourseSessionJpaRepository extends JpaRepository<CourseSessionEntity, Long> {

    List<CourseSessionEntity> findByCourseId(Long courseId);
    List<CourseSessionEntity> findByCourseIdIn(java.util.Collection<Long> courseIds);

    @Query("""
            select count(cs) > 0 from CourseSessionEntity cs
            where cs.room = :room and cs.dayOfWeek = :dayOfWeek
              and cs.startTime < :endTime and :startTime < cs.endTime
              and (:excludeId is null or cs.id <> :excludeId)
            """)
    boolean existsRoomTimeConflict(String room, String dayOfWeek, LocalTime startTime, LocalTime endTime, Long excludeId);

    @Modifying
    @Query("delete from CourseSessionEntity cs where cs.courseId = :courseId")
    int deleteByCourseId(Long courseId);

    @Query(value = """
            SELECT cs.id AS sessionId,
                   c.id AS courseId,
                   c.course_code AS courseCode,
                   cs.section AS section,
                   c.job_group AS job,
                   c.job_level AS position,
                   COALESCE(c.subject_name, c.title) AS title,
                   COALESCE(c.instructor, c.professor) AS professor,
                   CONCAT(cs.day_of_week, ' ', TIME_FORMAT(cs.start_time, '%H:%i'), '-', TIME_FORMAT(cs.end_time, '%H:%i')) AS classTime,
                   CASE WHEN c.price = 0 THEN '무료' ELSE CONCAT('₩', FORMAT(c.price, 0)) END AS price,
                   cs.enrolled_count AS enrolledCount,
                   COALESCE(c.capacity, cs.max_count) AS maxCount,
                   cs.day_of_week AS day,
                   TIME_FORMAT(cs.start_time, '%H:%i') AS startTime,
                   TIME_FORMAT(cs.end_time, '%H:%i') AS endTime,
                   '' AS dayNight,
                   CASE WHEN cs.enrolled_count >= COALESCE(c.capacity, cs.max_count) THEN '신청불가' ELSE '신청가능' END AS note,
                   COALESCE(c.status, 'OPEN') AS courseStatus
            FROM course_sessions cs
            JOIN courses c ON cs.course_id = c.id
            ORDER BY FIELD(cs.day_of_week, '월','화','수','목','금','토','일'), cs.start_time
            """, nativeQuery = true)
    List<CourseListProjection> findAllCourseRows();

    @Query(value = """
            SELECT c.course_code AS courseCode,
                   COUNT(*) AS enrolledCount
            FROM enrollments e
            JOIN course_sessions cs ON cs.id = e.course_session_id
            JOIN courses c ON c.id = cs.course_id
            WHERE e.status IN ('APPROVED','ENROLLED','RUNNING','REQUESTED')
            GROUP BY c.course_code
            """, nativeQuery = true)
    List<CourseEnrollmentCountProjection> findCourseEnrollmentCounts();
}
