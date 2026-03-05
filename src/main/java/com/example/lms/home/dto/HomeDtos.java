package com.example.lms.home.dto;

import java.time.LocalDateTime;

public class HomeDtos {
    public record CourseCardDto(
            Long courseId,
            String courseName,
            String summary
    ) {}

    public record HomePostSummaryDto(
            Long id,
            String title,
            LocalDateTime createdAt
    ) {}
}
