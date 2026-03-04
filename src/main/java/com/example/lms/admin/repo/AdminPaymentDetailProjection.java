package com.example.lms.admin.repo;

import java.time.LocalDateTime;

public interface AdminPaymentDetailProjection {
    Long getPaymentId();
    LocalDateTime getCreatedAt();
    Long getUserId();
    String getLoginId();
    String getUserName();
    String getType();
    Integer getAmount();
    String getMemo();
    Integer getPointBalance();
    Long getCourseId();
    String getCourseCode();
    String getCourseTitle();
    String getEnrollmentStatus();
}
