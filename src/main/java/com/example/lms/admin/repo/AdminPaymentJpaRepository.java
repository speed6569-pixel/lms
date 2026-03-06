package com.example.lms.admin.repo;

import com.example.lms.enrollment.entity.PointTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminPaymentJpaRepository extends JpaRepository<PointTransactionEntity, Long> {

    @Query(value = """
            SELECT pt.id AS paymentId,
                   pt.created_at AS createdAt,
                   u.id AS userId,
                   u.login_id AS loginId,
                   u.name AS userName,
                   pt.type AS type,
                   c.id AS courseId,
                   COALESCE(c.subject_code, c.course_code) AS courseCode,
                   COALESCE(c.subject_name, c.title) AS courseTitle,
                   pt.amount AS amount,
                   pt.memo AS memo,
                   pt.refund_status AS refundStatus,
                   pt.refund_requested_at AS refundRequestedAt
            FROM point_transactions pt
            JOIN users u ON u.id = pt.user_id
            LEFT JOIN courses c ON c.id = pt.course_id
            WHERE (:qUser IS NULL OR :qUser = '' OR u.login_id LIKE CONCAT('%', :qUser, '%') OR u.name LIKE CONCAT('%', :qUser, '%'))
              AND (:qCourse IS NULL OR :qCourse = '' OR COALESCE(c.subject_code, c.course_code) LIKE CONCAT('%', :qCourse, '%') OR COALESCE(c.subject_name, c.title) LIKE CONCAT('%', :qCourse, '%'))
              AND (:type IS NULL OR :type = '' OR pt.type = :type)
              AND (:fromDt IS NULL OR pt.created_at >= :fromDt)
              AND (:toDt IS NULL OR pt.created_at <= :toDt)
            ORDER BY pt.id DESC
            """, nativeQuery = true)
    List<AdminPaymentRowProjection> searchRows(@Param("qUser") String qUser,
                                               @Param("qCourse") String qCourse,
                                               @Param("type") String type,
                                               @Param("fromDt") LocalDateTime fromDt,
                                               @Param("toDt") LocalDateTime toDt);

    @Query(value = """
            SELECT pt.id AS paymentId,
                   pt.created_at AS createdAt,
                   u.id AS userId,
                   u.login_id AS loginId,
                   u.name AS userName,
                   pt.type AS type,
                   pt.amount AS amount,
                   pt.memo AS memo,
                   u.point_balance AS pointBalance,
                   c.id AS courseId,
                   COALESCE(c.subject_code, c.course_code) AS courseCode,
                   COALESCE(c.subject_name, c.title) AS courseTitle,
                   e.status AS enrollmentStatus,
                   pt.refund_status AS refundStatus,
                   pt.refund_requested_at AS refundRequestedAt,
                   pt.refund_processed_at AS refundProcessedAt,
                   pt.refund_reject_reason AS refundRejectReason
            FROM point_transactions pt
            JOIN users u ON u.id = pt.user_id
            LEFT JOIN courses c ON c.id = pt.course_id
            LEFT JOIN enrollments e ON e.user_id = pt.user_id AND e.course_id = pt.course_id
            WHERE pt.id = :id
            ORDER BY e.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<AdminPaymentDetailProjection> findDetail(@Param("id") Long id);
}
