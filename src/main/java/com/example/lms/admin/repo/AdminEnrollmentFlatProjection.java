package com.example.lms.admin.repo;

public interface AdminEnrollmentFlatProjection {
    Long getEnrollmentId();
    String getUsername();
    String getName();
    String getSubjectCode();
    String getCourseName();
    String getDay();
    String getStartTime();
    String getEndTime();
    Integer getPrice();
    String getPaymentStatus();
    String getStatus();
    String getAppliedAt();
}
