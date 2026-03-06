package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.PointTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransactionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pt from PointTransactionEntity pt where pt.id = :id")
    Optional<PointTransactionEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(pt.amount), 0) from PointTransactionEntity pt where pt.userId = :userId and pt.courseId = :courseId and pt.type = com.example.lms.enrollment.entity.PointTransactionType.SPEND")
    Integer sumSpendByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("select coalesce(sum(pt.amount), 0) from PointTransactionEntity pt where pt.userId = :userId and pt.courseId = :courseId and pt.type = com.example.lms.enrollment.entity.PointTransactionType.REFUND")
    Integer sumRefundByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("select coalesce(sum(case when pt.type = com.example.lms.enrollment.entity.PointTransactionType.SPEND then pt.amount when pt.type = com.example.lms.enrollment.entity.PointTransactionType.REFUND then -pt.amount else 0 end), 0) from PointTransactionEntity pt where pt.userId = :userId and pt.courseId = :courseId")
    Integer netPaidByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query(value = """
            SELECT pt.id AS id,
                   pt.user_id AS userId,
                   pt.course_id AS courseId,
                   pt.created_at AS createdAt,
                   COALESCE(c.subject_name, c.title) AS courseTitle,
                   COALESCE(c.subject_code, c.course_code) AS subjectCode,
                   pt.amount AS amount,
                   pt.type AS type,
                   pt.refund_status AS refundStatus,
                   pt.refund_requested_at AS refundRequestedAt,
                   pt.refund_processed_at AS refundProcessedAt,
                   pt.refund_reject_reason AS refundRejectReason,
                   pt.balance_after AS balanceAfter,
                   pt.memo AS memo
            FROM point_transactions pt
            LEFT JOIN courses c ON c.id = pt.course_id
            WHERE pt.user_id = :userId
            ORDER BY pt.created_at DESC, pt.id DESC
            """, nativeQuery = true)
    List<MyPointTransactionProjection> findHistoryByUserId(@Param("userId") Long userId);
}
