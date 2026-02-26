package com.example.lms.enrollment.repo;

public interface CourseListProjection {
    String getCourseCode();
    String getSection();
    String getJob();
    String getPosition();
    String getTitle();
    String getProfessor();
    String getClassTime();
    String getPrice();
    Integer getEnrolledCount();
    Integer getMaxCount();
    String getDay();
    String getStartTime();
    String getEndTime();
    String getDayNight();
    String getNote();
}
