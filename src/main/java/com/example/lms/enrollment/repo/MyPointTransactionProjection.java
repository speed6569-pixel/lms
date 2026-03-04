package com.example.lms.enrollment.repo;

import java.time.LocalDateTime;

public interface MyPointTransactionProjection {
    Long getId();
    LocalDateTime getCreatedAt();
    String getCourseTitle();
    String getSubjectCode();
    Integer getAmount();
    String getType();
    Integer getBalanceAfter();
    String getMemo();
}
