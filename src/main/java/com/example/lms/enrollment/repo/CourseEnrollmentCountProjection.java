package com.example.lms.enrollment.repo;

public interface CourseEnrollmentCountProjection {
    String getCourseCode();
    Integer getEnrolledCount();
}
