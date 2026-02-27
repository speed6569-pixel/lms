package com.example.lms.payments.repo;

import com.example.lms.payments.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);
    Optional<PaymentEntity> findTopByUserIdAndCourseIdAndStatusOrderByIdDesc(Long userId, Long courseId, String status);
    Optional<PaymentEntity> findByIdAndUserId(Long id, Long userId);
}
