package com.example.lms.enrollment.repo;

public interface TimetableBlockProjection {
    String getCourseCode();
    String getSection();
    String getTitle();
    String getProfessor();
    String getRoom();
    String getDay();
    String getStartTime();
    String getEndTime();
    String getStatus();
}
