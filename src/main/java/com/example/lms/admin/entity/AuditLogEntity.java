package com.example.lms.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id")
    private Long actorUserId;
    private String action;
    @Column(name = "target_type")
    private String targetType;
    @Column(name = "target_id")
    private Long targetId;
    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;
    @Column(name = "after_json", columnDefinition = "TEXT")
    private String afterJson;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public void setAction(String action) { this.action = action; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
