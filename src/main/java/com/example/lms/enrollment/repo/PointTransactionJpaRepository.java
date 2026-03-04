package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.PointTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransactionEntity, Long> {

    @Query(value = """
            SELECT pt.id AS id,
                   pt.created_at AS createdAt,
                   COALESCE(c.subject_name, c.title) AS courseTitle,
                   COALESCE(c.subject_code, c.course_code) AS subjectCode,
                   pt.amount AS amount,
                   pt.type AS type,
                   pt.balance_after AS balanceAfter,
                   pt.memo AS memo
            FROM point_transactions pt
            LEFT JOIN courses c ON c.id = pt.course_id
            WHERE pt.user_id = :userId
            ORDER BY pt.created_at DESC, pt.id DESC
            """, nativeQuery = true)
    List<MyPointTransactionProjection> findHistoryByUserId(@Param("userId") Long userId);
}
