package com.example.lms.enrollment.repo;

import com.example.lms.admin.repo.AdminUserStatsProjection;
import com.example.lms.enrollment.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByLoginId(String loginId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
    boolean existsByLoginId(String loginId);
    long countByRole(String role);
    List<UserEntity> findByLoginIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByIdDesc(String loginId, String name);

    @Query(value = """
            SELECT u.id AS id,
                   u.login_id AS loginId,
                   u.name AS name,
                   COALESCE(u.status, CASE WHEN u.enabled = 1 THEN 'ACTIVE' ELSE 'BLOCKED' END) AS status,
                   u.role AS role,
                   u.created_at AS createdAt,
                   COALESCE(p.total_payment, 0) AS totalPayment,
                   COALESCE(e.running_count, 0) AS runningCourseCount
            FROM users u
            LEFT JOIN (
                SELECT user_id,
                       SUM(CASE WHEN status = 'PAID' THEN amount WHEN status = 'REFUNDED' THEN -amount ELSE 0 END) AS total_payment
                FROM payments
                GROUP BY user_id
            ) p ON p.user_id = u.id
            LEFT JOIN (
                SELECT user_id, COUNT(*) AS running_count
                FROM enrollments
                WHERE status IN ('RUNNING','APPROVED','ENROLLED')
                GROUP BY user_id
            ) e ON e.user_id = u.id
            WHERE (:q IS NULL OR :q = '' OR u.login_id LIKE CONCAT('%', :q, '%') OR u.name LIKE CONCAT('%', :q, '%'))
            ORDER BY u.id DESC
            """, nativeQuery = true)
    List<AdminUserStatsProjection> searchAdminUserStats(@Param("q") String q);
}
