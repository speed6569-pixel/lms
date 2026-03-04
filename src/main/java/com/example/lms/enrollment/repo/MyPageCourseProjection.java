package com.example.lms.enrollment.repo;

public interface MyPageCourseProjection {
    Long getCourseId();
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
