package com.example.lms.enrollment.repo;

public interface MyEnrollmentHistoryProjection {
    Long getEnrollmentId();
    String getCourseCode();
    String getTitle();
    String getStatus();
    String getAppliedAt();
}
