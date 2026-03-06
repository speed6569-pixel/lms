package com.example.lms.enrollment.repo;

import java.time.LocalDateTime;

public interface MyPointTransactionProjection {
    Long getId();
    Long getUserId();
    Long getCourseId();
    LocalDateTime getCreatedAt();
    String getCourseTitle();
    String getSubjectCode();
    Integer getAmount();
    String getType();
    String getRefundStatus();
    LocalDateTime getRefundRequestedAt();
    LocalDateTime getRefundProcessedAt();
    String getRefundRejectReason();
    Integer getBalanceAfter();
    String getMemo();
}
