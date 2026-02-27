package com.example.lms.admin.dto;

import java.time.LocalDate;

public class AdminDtos {
    public record CourseCreateRequest(String courseCode, String title, String description, String professor, Integer price, Integer maxCount, String classTime) {}
    public record CourseUpdateRequest(String title, String description, String professor, Integer price, Integer maxCount, String classTime, Boolean active) {}
    public record SessionCreateRequest(String section, String dayOfWeek, String startTime, String endTime, String room, Integer maxCount) {}
    public record EnrollmentDecisionRequest(String reason) {}
    public record AttendanceCheckRequest(Long enrollmentId, LocalDate sessionDate, String status, Integer minutesAttended, Integer minutesTotal) {}
    public record ProgressUpdateRequest(Long enrollmentId, String unitId, Double progressPercent, Boolean completed) {}
    public record UserRoleRequest(String role) {}
    public record UserEnabledRequest(Boolean enabled) {}
}
