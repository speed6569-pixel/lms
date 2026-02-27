package com.example.lms.enrollment.repo;

public interface MyPageCourseProjection {
    String getCourseCode();
    String getSection();
    String getTitle();
    String getProfessor();
    String getClassTime();
    String getPrice();
    Integer getEnrolledCount();
    Integer getMaxCount();
    String getStatus();
}
