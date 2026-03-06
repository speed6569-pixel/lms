package com.example.lms.enrollment.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_transactions")
public class PointTransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id")
    private Long courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PointTransactionType type;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "memo", length = 255)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 30)
    private RefundStatus refundStatus;

    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Column(name = "refund_processed_at")
    private LocalDateTime refundProcessedAt;

    @Column(name = "refund_reject_reason", length = 255)
    private String refundRejectReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCourseId() { return courseId; }
    public PointTransactionType getType() { return type; }
    public Integer getAmount() { return amount; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public String getMemo() { return memo; }
    public RefundStatus getRefundStatus() { return refundStatus; }
    public LocalDateTime getRefundRequestedAt() { return refundRequestedAt; }
    public LocalDateTime getRefundProcessedAt() { return refundProcessedAt; }
    public String getRefundRejectReason() { return refundRejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public void setType(PointTransactionType type) { this.type = type; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public void setMemo(String memo) { this.memo = memo; }
    public void setRefundStatus(RefundStatus refundStatus) { this.refundStatus = refundStatus; }
    public void setRefundRequestedAt(LocalDateTime refundRequestedAt) { this.refundRequestedAt = refundRequestedAt; }
    public void setRefundProcessedAt(LocalDateTime refundProcessedAt) { this.refundProcessedAt = refundProcessedAt; }
    public void setRefundRejectReason(String refundRejectReason) { this.refundRejectReason = refundRejectReason; }
}
