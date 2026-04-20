package com.example.lms.admin.service;

import com.example.lms.admin.repo.AdminPaymentDetailProjection;
import com.example.lms.admin.repo.AdminPaymentJpaRepository;
import com.example.lms.admin.repo.AdminPaymentRowProjection;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.service.RefundService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AdminPaymentService {

    private final AdminPaymentJpaRepository adminPaymentJpaRepository;
    private final PointTransactionJpaRepository pointTransactionJpaRepository;
    private final RefundService refundService;

    public AdminPaymentService(AdminPaymentJpaRepository adminPaymentJpaRepository,
                               PointTransactionJpaRepository pointTransactionJpaRepository,
                               RefundService refundService) {
        this.adminPaymentJpaRepository = adminPaymentJpaRepository;
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
        this.refundService = refundService;
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentRowProjection> search(String qUser, String qCourse, String type, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from == null ? null : from.atStartOfDay();
        LocalDateTime toDt = to == null ? null : to.atTime(LocalTime.MAX);
        return adminPaymentJpaRepository.searchRows(qUser, qCourse, type, fromDt, toDt);
    }

    @Transactional(readOnly = true, propagation = Propagation.NOT_SUPPORTED)
    public PaymentDetailView getDetail(Long id) {
        AdminPaymentDetailProjection item = adminPaymentJpaRepository.findDetail(id)
                .orElseThrow(() -> new IllegalArgumentException("결제/포인트 내역을 찾을 수 없습니다."));

        int progressPercent = 0;
        boolean refundable = false;
        String refundableReason = "";
        boolean under30Progress = false;
        boolean within3Days = false;

        var entity = pointTransactionJpaRepository.findById(id).orElse(null);
        if (entity != null) {
            var eligibility = refundService.evaluateEligibility(entity);
            progressPercent = eligibility.progressPercent();
            refundable = eligibility.refundable();
            refundableReason = eligibility.reason();
            under30Progress = eligibility.under30Progress();
            within3Days = eligibility.within3Days();
        }

        return new PaymentDetailView(item, progressPercent, refundable, refundableReason, under30Progress, within3Days);
    }

    @Transactional
    public void approveRefund(Long id) {
        refundService.approveRefund(id);
    }

    @Transactional
    public void rejectRefund(Long id, String reason) {
        refundService.rejectRefund(id, reason);
    }

    public record PaymentDetailView(AdminPaymentDetailProjection item,
                                    int progressPercent,
                                    boolean refundable,
                                    String refundableReason,
                                    boolean under30Progress,
                                    boolean within3Days) {
    }
}
