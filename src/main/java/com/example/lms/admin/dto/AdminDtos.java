package com.example.lms.admin.dto;

import java.time.LocalDate;

public class AdminDtos {
    public record CourseCreateRequest(String subjectCode, String jobGroup, String jobLevel, String subjectName, String instructor, Integer price, Integer capacity, String status, String description, String dayMode, java.util.List<String> days, String startDay, String endDay, String startTime, String endTime, java.util.List<CourseSessionInput> sessions) {}
    public record CourseUpdateRequest(String subjectName, String instructor, Integer price, Integer capacity, String status, String jobGroup, String jobLevel, String classTime, Boolean active) {}
    public record CourseSessionInput(String dayOfWeek, String startTime, String endTime, String room) {}
    public record SessionCreateRequest(String section, String dayOfWeek, String startTime, String endTime, String room, Integer maxCount) {}
    public record EnrollmentDecisionRequest(String reason) {}
    public record AttendanceCheckRequest(Long enrollmentId, LocalDate sessionDate, String status, Integer minutesAttended, Integer minutesTotal) {}
    public record ProgressUpdateRequest(Long enrollmentId, String unitId, Double progressPercent, Boolean completed) {}
    public record UserRoleRequest(String role) {}
    public record UserEnabledRequest(Boolean enabled) {}
}
