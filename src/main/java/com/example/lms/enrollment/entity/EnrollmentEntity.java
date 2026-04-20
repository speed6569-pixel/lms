package com.example.lms.enrollment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class EnrollmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_session_id", nullable = false)
    private Long courseSessionId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseSessionId() { return courseSessionId; }
    public String getStatus() { return status; }
    public Long getCourseId() { return courseId; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public LocalDateTime getAppliedAt() { return appliedAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setCourseSessionId(Long courseSessionId) { this.courseSessionId = courseSessionId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public void setStatus(String status) { this.status = status; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
