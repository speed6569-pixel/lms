package com.example.lms.enrollment.repo;

import com.example.lms.admin.repo.AdminEnrollmentFlatProjection;
import com.example.lms.enrollment.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, Long> {

    boolean existsByUserIdAndCourseSessionIdAndStatus(Long userId, Long courseSessionId, String status);

    boolean existsByUserIdAndCourseIdAndStatusIn(Long userId, Long courseId, java.util.Collection<String> statuses);

    long countByCourseIdAndStatusIn(Long courseId, java.util.Collection<String> statuses);

    boolean existsByUserIdAndCourseSessionIdAndStatusIn(Long userId, Long courseSessionId, java.util.Collection<String> statuses);

    java.util.List<EnrollmentEntity> findByStatusOrderByIdAsc(String status);

    EnrollmentEntity findTopByUserIdAndCourseSessionIdAndStatusOrderByIdDesc(Long userId, Long courseSessionId, String status);

    java.util.Optional<EnrollmentEntity> findByIdAndUserId(Long id, Long userId);

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
                   COALESCE(cs.section, '01') AS section,
                   COALESCE(c.subject_name, c.title) AS title,
                   COALESCE(c.instructor, c.professor) AS professor,
                   (
                     SELECT GROUP_CONCAT(CONCAT(cs2.day_of_week, ' ', TIME_FORMAT(cs2.start_time, '%H:%i'), '-', TIME_FORMAT(cs2.end_time, '%H:%i'))
                            ORDER BY FIELD(cs2.day_of_week,'월','화','수','목','금','토','일'), cs2.start_time SEPARATOR ', ')
                     FROM course_sessions cs2
                     WHERE cs2.course_id = c.id
                   ) AS classTime,
                   CASE WHEN c.price = 0 THEN '무료' ELSE CONCAT('₩', FORMAT(c.price, 0)) END AS price,
                   (
                     SELECT COUNT(*)
                     FROM enrollments e2
                     WHERE e2.course_id = c.id
                       AND e2.status IN ('APPROVED','RUNNING')
                   ) AS enrolledCount,
                   COALESCE(c.capacity, cs.max_count, 0) AS maxCount,
                   e.status AS status
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            LEFT JOIN course_sessions cs ON cs.id = e.course_session_id
            WHERE e.user_id = :userId
              AND e.status IN (:statuses)
            ORDER BY e.id DESC
            """, nativeQuery = true)
    List<MyPageCourseProjection> findMyCoursesByStatuses(@Param("userId") Long userId,
                                                          @Param("statuses") Set<String> statuses);

    @Query(value = """
            SELECT COUNT(*)
            FROM enrollments e
            WHERE e.user_id = :userId
              AND e.status = 'ENROLLED'
            """, nativeQuery = true)
    long countEnrolledCourses(@Param("userId") Long userId);

    @Query(value = """
            SELECT ROUND(AVG(
                CASE ar.status
                    WHEN 'PRESENT' THEN 100
                    WHEN 'LATE' THEN 50
                    WHEN 'EXCUSED' THEN 100
                    ELSE 0
                END
            ), 1)
            FROM attendance_records ar
            JOIN enrollments e ON e.id = ar.enrollment_id
            WHERE e.user_id = :userId
              AND e.status = 'ENROLLED'
            """, nativeQuery = true)
    Double findAttendanceRate(@Param("userId") Long userId);

    @Query(value = """
            SELECT ROUND(AVG(pr.progress_percent), 1)
            FROM progress_records pr
            JOIN enrollments e ON e.id = pr.enrollment_id
            WHERE e.user_id = :userId
              AND e.status = 'ENROLLED'
            """, nativeQuery = true)
    Double findProgressRate(@Param("userId") Long userId);

    @Query(value = """
            SELECT c.course_code AS courseCode,
                   cs.section AS section,
                   COALESCE(c.subject_name, c.title) AS title,
                   COALESCE(c.instructor, c.professor) AS professor,
                   cs.room AS room,
                   cs.day_of_week AS day,
                   TIME_FORMAT(cs.start_time, '%H:%i') AS startTime,
                   TIME_FORMAT(cs.end_time, '%H:%i') AS endTime,
                   e.status AS status
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            JOIN course_sessions cs ON cs.course_id = e.course_id
            WHERE e.user_id = :userId
              AND e.status IN (:statuses)
            ORDER BY FIELD(cs.day_of_week, '월','화','수','목','금'), cs.start_time
            """, nativeQuery = true)
    List<TimetableBlockProjection> findTimetableByStatuses(@Param("userId") Long userId,
                                                           @Param("statuses") Set<String> statuses);

    @Modifying
    @Query(value = """
            DELETE ar FROM attendance_records ar
            JOIN enrollments e ON e.id = ar.enrollment_id
            WHERE e.user_id = :userId
            """, nativeQuery = true)
    int deleteAttendanceByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            DELETE pr FROM progress_records pr
            JOIN enrollments e ON e.id = pr.enrollment_id
            WHERE e.user_id = :userId
            """, nativeQuery = true)
    int deleteProgressByUserId(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM enrollments WHERE user_id = :userId", nativeQuery = true)
    int deleteEnrollmentsByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT e.id AS enrollmentId,
                   c.course_code AS courseCode,
                   COALESCE(c.subject_name, c.title) AS title,
                   e.status AS status,
                   DATE_FORMAT(e.applied_at, '%Y-%m-%d %H:%i') AS appliedAt
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.user_id = :userId
              AND e.status IN ('APPLIED','WAITLIST','APPROVED','RUNNING','CANCEL_REQUESTED')
            ORDER BY e.id DESC
            """, nativeQuery = true)
    List<MyEnrollmentHistoryProjection> findMyEnrollmentHistory(@Param("userId") Long userId);

    @Query(value = """
            SELECT e.id AS enrollmentId,
                   u.login_id AS username,
                   u.name AS name,
                   COALESCE(c.subject_code, c.course_code) AS subjectCode,
                   COALESCE(c.subject_name, c.title) AS courseName,
                   cs.day_of_week AS day,
                   TIME_FORMAT(cs.start_time, '%H:%i') AS startTime,
                   TIME_FORMAT(cs.end_time, '%H:%i') AS endTime,
                   c.price AS price,
                   CASE
                     WHEN c.price = 0 THEN 'FREE'
                     WHEN COALESCE(paid.has_refunded, 0) = 1 AND COALESCE(paid.paid_amount, 0) <= 0 THEN 'REFUNDED'
                     WHEN COALESCE(paid.paid_amount, 0) > 0 THEN 'PAID'
                     ELSE 'UNPAID'
                   END AS paymentStatus,
                   e.status AS status,
                   DATE_FORMAT(COALESCE(e.applied_at, e.enrolled_at, e.updated_at), '%Y-%m-%d %H:%i') AS appliedAt
            FROM enrollments e
            JOIN users u ON u.id = e.user_id
            JOIN courses c ON c.id = e.course_id
            JOIN course_sessions cs ON cs.course_id = e.course_id
            LEFT JOIN (
                SELECT user_id,
                       SUM(CASE WHEN status='PAID' THEN amount WHEN status='REFUNDED' THEN -amount ELSE 0 END) AS paid_amount,
                       MAX(CASE WHEN status='REFUNDED' THEN 1 ELSE 0 END) AS has_refunded
                FROM payments
                GROUP BY user_id
            ) paid ON paid.user_id = e.user_id
            WHERE e.status = :status
            ORDER BY e.id DESC, FIELD(cs.day_of_week, '월','화','수','목','금','토','일'), cs.start_time
            """, nativeQuery = true)
    List<AdminEnrollmentFlatProjection> findAdminEnrollmentRowsByStatus(@Param("status") String status);

    @Modifying
    @Query(value = """
            DELETE ar FROM attendance_records ar
            JOIN enrollments e ON e.id = ar.enrollment_id
            JOIN course_sessions cs ON cs.id = e.course_session_id
            WHERE cs.course_id = :courseId
            """, nativeQuery = true)
    int deleteAttendanceByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            DELETE pr FROM progress_records pr
            JOIN enrollments e ON e.id = pr.enrollment_id
            JOIN course_sessions cs ON cs.id = e.course_session_id
            WHERE cs.course_id = :courseId
            """, nativeQuery = true)
    int deleteProgressByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            DELETE e FROM enrollments e
            JOIN course_sessions cs ON cs.id = e.course_session_id
            WHERE cs.course_id = :courseId
            """, nativeQuery = true)
    int deleteEnrollmentsByCourseId(@Param("courseId") Long courseId);
}

