package com.example.lms.admin.dto;

public record AdminEnrollmentRowDto(
        Long enrollmentId,
        String username,
        String name,
        String subjectCode,
        String courseName,
        String scheduleText,
        Integer price,
        String paymentStatus,
        String status,
        String appliedAt
) {}
