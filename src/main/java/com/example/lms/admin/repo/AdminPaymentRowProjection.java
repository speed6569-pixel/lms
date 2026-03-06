package com.example.lms.admin.repo;

import java.time.LocalDateTime;

public interface AdminPaymentRowProjection {
    Long getPaymentId();
    LocalDateTime getCreatedAt();
    Long getUserId();
    String getLoginId();
    String getUserName();
    String getType();
    Long getCourseId();
    String getCourseCode();
    String getCourseTitle();
    Integer getAmount();
    String getMemo();
    String getRefundStatus();
    LocalDateTime getRefundRequestedAt();
}
