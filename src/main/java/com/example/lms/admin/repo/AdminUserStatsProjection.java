package com.example.lms.admin.repo;

import java.time.LocalDateTime;

public interface AdminUserStatsProjection {
    Long getId();
    String getLoginId();
    String getName();
    String getStatus();
    String getRole();
    LocalDateTime getCreatedAt();
    Long getTotalPayment();
    Long getRunningCourseCount();
}
