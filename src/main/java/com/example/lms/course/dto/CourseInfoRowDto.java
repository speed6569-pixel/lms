package com.example.lms.course.dto;

public record CourseInfoRowDto(
        String subjectCode,
        String jobGroup,
        String jobLevel,
        String courseName,
        String instructor,
        String scheduleText,
        String price,
        int enrolledCount,
        int capacity,
        String openedDate
) {
}
