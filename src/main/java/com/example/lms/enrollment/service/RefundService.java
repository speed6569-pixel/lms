package com.example.lms.enrollment.service;

import com.example.lms.enrollment.entity.*;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.learn.service.LearnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final PointTransactionJpaRepository pointTransactionJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final LearnService learnService;

    public RefundService(PointTransactionJpaRepository pointTransactionJpaRepository,
                         UserJpaRepository userJpaRepository,
                         EnrollmentJpaRepository enrollmentJpaRepository,
                         LearnService learnService) {
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.learnService = learnService;
    }

    public RefundEligibility evaluateEligibility(PointTransactionEntity payment) {
        if (payment.getType() != PointTransactionType.SPEND) {
            return RefundEligibility.notAllowed("강의 결제 건만 환불 신청할 수 있습니다.", 0, false);
        }
        if (payment.getCourseId() == null) {
            return RefundEligibility.notAllowed("강의 결제 건만 환불 신청할 수 있습니다.", 0, false);
        }
        if (payment.getRefundStatus() == RefundStatus.REFUND_REQUESTED) {
            return RefundEligibility.notAllowed("이미 환불 신청된 결제 건입니다.", 0, false);
        }
        if (payment.getRefundStatus() == RefundStatus.REFUND_APPROVED) {
            return RefundEligibility.notAllowed("이미 환불 완료된 결제 건입니다.", 0, false);
        }

        int progress = 0;
        try {
            progress = learnService.getCourseProgress(payment.getUserId(), payment.getCourseId()).percent();
        } catch (Exception ignored) {
        }

        boolean progressRule = progress < 30;
        boolean dateRule = payment.getCreatedAt() != null && payment.getCreatedAt().plusDays(3).isAfter(LocalDateTime.now());
        boolean refundable = progressRule || dateRule;

        if (!refundable) {
            return RefundEligibility.notAllowed("환불 불가(진도 30% 이상 또는 구매 후 3일 초과)", progress, false);
        }
        return new RefundEligibility(true, "환불 신청 가능", progress, dateRule, progressRule);
    }

    @Transactional
    public void requestRefund(Long loginUserId, Long paymentId) {
        PointTransactionEntity payment = pointTransactionJpaRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        if (!payment.getUserId().equals(loginUserId)) {
            throw new IllegalArgumentException("본인 결제 건만 환불 신청할 수 있습니다.");
        }

        RefundEligibility eligibility = evaluateEligibility(payment);
        if (!eligibility.refundable()) throw new IllegalStateException(eligibility.reason());

        payment.setRefundStatus(RefundStatus.REFUND_REQUESTED);
        payment.setRefundRequestedAt(LocalDateTime.now());
        payment.setRefundRejectReason(null);
        pointTransactionJpaRepository.save(payment);
        log.info("[refund] requested paymentId={}, userId={}, courseId={}", payment.getId(), payment.getUserId(), payment.getCourseId());
    }

    @Transactional
    public void approveRefund(Long paymentId) {
        PointTransactionEntity payment = pointTransactionJpaRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        if (payment.getRefundStatus() != RefundStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 승인 대기 상태의 결제 건만 승인할 수 있습니다.");
        }

        RefundEligibility eligibility = evaluateEligibility(payment);
        if (!eligibility.refundable()) throw new IllegalStateException("환불 조건 미충족: " + eligibility.reason());

        UserEntity user = userJpaRepository.findByIdForUpdate(payment.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int nextBalance = (user.getPointBalance() == null ? 0 : user.getPointBalance()) + (payment.getAmount() == null ? 0 : payment.getAmount());
        user.setPointBalance(nextBalance);

        PointTransactionEntity refundTx = new PointTransactionEntity();
        refundTx.setUserId(payment.getUserId());
        refundTx.setCourseId(payment.getCourseId());
        refundTx.setType(PointTransactionType.REFUND);
        refundTx.setAmount(payment.getAmount());
        refundTx.setBalanceAfter(nextBalance);
        refundTx.setMemo("환불 승인");
        refundTx.setRefundStatus(RefundStatus.REFUND_APPROVED);
        pointTransactionJpaRepository.save(refundTx);

        payment.setRefundStatus(RefundStatus.REFUND_APPROVED);
        payment.setRefundProcessedAt(LocalDateTime.now());
        pointTransactionJpaRepository.save(payment);
        log.info("[refund] approved paymentId={}, userId={}, amount={}", payment.getId(), payment.getUserId(), payment.getAmount());

        enrollmentJpaRepository.findByUserIdAndCourseId(payment.getUserId(), payment.getCourseId())
                .ifPresent(en -> {
                    en.setStatus("REFUNDED");
                    en.setCanceledAt(LocalDateTime.now());
                    enrollmentJpaRepository.save(en);
                });
    }

    @Transactional
    public void rejectRefund(Long paymentId, String reason) {
        PointTransactionEntity payment = pointTransactionJpaRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        if (payment.getRefundStatus() != RefundStatus.REFUND_REQUESTED) {
            throw new IllegalStateException("환불 승인 대기 상태의 결제 건만 거절할 수 있습니다.");
        }

        payment.setRefundStatus(RefundStatus.REFUND_REJECTED);
        payment.setRefundProcessedAt(LocalDateTime.now());
        payment.setRefundRejectReason(reason == null || reason.isBlank() ? "환불 조건 미충족" : reason.trim());
        pointTransactionJpaRepository.save(payment);
        log.info("[refund] rejected paymentId={}, userId={}, reason={}", payment.getId(), payment.getUserId(), payment.getRefundRejectReason());
    }

    public record RefundEligibility(boolean refundable,
                                    String reason,
                                    int progressPercent,
                                    boolean within3Days,
                                    boolean under30Progress) {
        static RefundEligibility notAllowed(String reason, int progressPercent, boolean within3Days) {
            return new RefundEligibility(false, reason, progressPercent, within3Days, progressPercent < 30);
        }
    }
}
