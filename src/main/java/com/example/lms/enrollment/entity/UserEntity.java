package com.example.lms.enrollment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "role", nullable = false)
    private String role;

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public String getRole() { return role; }

    public void setLoginId(String loginId) { this.loginId = loginId; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setName(String name) { this.name = name; }
    public void setRole(String role) { this.role = role; }
}
