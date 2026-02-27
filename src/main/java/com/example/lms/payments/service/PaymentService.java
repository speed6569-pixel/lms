package com.example.lms.payments.service;

import com.example.lms.admin.entity.CourseEntity;
import com.example.lms.admin.repo.CourseJpaRepository;
import com.example.lms.payments.entity.PaymentEntity;
import com.example.lms.payments.repo.PaymentJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentService {
    private final PaymentJpaRepository paymentJpaRepository;
    private final CourseJpaRepository courseJpaRepository;

    public PaymentService(PaymentJpaRepository paymentJpaRepository, CourseJpaRepository courseJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
    }

    @Transactional
    public Map<String, Object> prepare(Long userId, Long courseId, String provider) {
        CourseEntity c = courseJpaRepository.findById(courseId).orElseThrow();
        long amount = c.getPrice() == null ? 0 : c.getPrice();
        if (amount <= 0) throw new IllegalArgumentException("무료 강의는 결제가 필요 없습니다.");

        if (paymentJpaRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, "PAID")) {
            throw new IllegalArgumentException("이미 결제 완료된 강의입니다.");
        }

        PaymentEntity p = new PaymentEntity();
        p.setUserId(userId);
        p.setCourseId(courseId);
        p.setAmount(amount);
        p.setStatus("PENDING");
        p.setPaymentProvider(provider == null || provider.isBlank() ? "CARD" : provider);
        paymentJpaRepository.save(p);

        return Map.of("paymentId", p.getId(), "amount", p.getAmount(), "status", p.getStatus());
    }

    @Transactional
    public Map<String, Object> confirm(Long userId, Long paymentId, String providerTxId) {
        PaymentEntity p = paymentJpaRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("결제건을 찾을 수 없습니다."));

        if (!"PENDING".equalsIgnoreCase(p.getStatus())) {
            throw new IllegalArgumentException("결제 확정 가능한 상태가 아닙니다.");
        }

        p.setStatus("PAID");
        p.setProviderTxId(providerTxId == null || providerTxId.isBlank() ? "SANDBOX-" + paymentId : providerTxId);
        p.setPaidAt(LocalDateTime.now());
        paymentJpaRepository.save(p);

        return Map.of("success", true, "paymentId", p.getId(), "status", p.getStatus());
    }

    @Transactional(readOnly = true)
    public boolean hasPaid(Long userId, Long courseId) {
        return paymentJpaRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, "PAID");
    }
}
