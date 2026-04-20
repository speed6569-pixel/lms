package com.example.lms.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
    private String status;
    @Column(name = "minutes_attended")
    private Integer minutesAttended;
    @Column(name = "minutes_total")
    private Integer minutesTotal;
    @Column(name = "recorded_by")
    private String recordedBy;
    @Column(name = "recorded_at", insertable = false, updatable = false)
    private LocalDateTime recordedAt;

    public Long getId() { return id; }
    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getMinutesAttended() { return minutesAttended; }
    public void setMinutesAttended(Integer minutesAttended) { this.minutesAttended = minutesAttended; }
    public Integer getMinutesTotal() { return minutesTotal; }
    public void setMinutesTotal(Integer minutesTotal) { this.minutesTotal = minutesTotal; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
}
