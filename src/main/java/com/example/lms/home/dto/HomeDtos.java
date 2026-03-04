package com.example.lms.home.dto;

public class HomeDtos {
    public record CourseCardDto(
            Long courseId,
            String courseName,
            String summary
    ) {}

    public record NoticeSummaryDto(
            Long id,
            String title,
            String summary
    ) {}
}
