package com.example.lms.admin.service;

import com.example.lms.admin.repo.AdminPaymentDetailProjection;
import com.example.lms.admin.repo.AdminPaymentJpaRepository;
import com.example.lms.admin.repo.AdminPaymentRowProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AdminPaymentService {

    private final AdminPaymentJpaRepository adminPaymentJpaRepository;

    public AdminPaymentService(AdminPaymentJpaRepository adminPaymentJpaRepository) {
        this.adminPaymentJpaRepository = adminPaymentJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentRowProjection> search(String qUser, String qCourse, String type, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from == null ? null : from.atStartOfDay();
        LocalDateTime toDt = to == null ? null : to.atTime(LocalTime.MAX);
        return adminPaymentJpaRepository.searchRows(qUser, qCourse, type, fromDt, toDt);
    }

    @Transactional(readOnly = true)
    public AdminPaymentDetailProjection getDetail(Long id) {
        return adminPaymentJpaRepository.findDetail(id)
                .orElseThrow(() -> new IllegalArgumentException("결제/포인트 내역을 찾을 수 없습니다."));
    }
}
