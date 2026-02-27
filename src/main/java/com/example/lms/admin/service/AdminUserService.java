package com.example.lms.admin.service;

import com.example.lms.admin.dto.AdminUserDtos;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AdminUserService {

    private final UserJpaRepository userJpaRepository;

    public AdminUserService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> searchUsers(String q) {
        if (q == null || q.isBlank()) return userJpaRepository.findAll().stream().sorted((a,b)->Long.compare(b.getId(), a.getId())).toList();
        return userJpaRepository.findByLoginIdContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByIdDesc(q.trim(), q.trim());
    }

    @Transactional
    public UserEntity updateUser(Long targetUserId, String adminLoginId, AdminUserDtos.UserUpdateRequest req) {
        UserEntity admin = userJpaRepository.findByLoginId(adminLoginId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));

        UserEntity target = userJpaRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String role = req.role() == null ? target.getRole() : req.role().trim();
        String status = req.status() == null ? target.getStatus() : req.status().trim().toUpperCase();

        if (!"ROLE_USER".equals(role) && !"ROLE_ADMIN".equals(role)) {
            throw new IllegalArgumentException("role은 ROLE_USER 또는 ROLE_ADMIN 이어야 합니다.");
        }
        if (!"ACTIVE".equals(status) && !"BLOCKED".equals(status)) {
            throw new IllegalArgumentException("status는 ACTIVE 또는 BLOCKED 이어야 합니다.");
        }

        if (admin.getId().equals(target.getId()) && "BLOCKED".equals(status)) {
            throw new IllegalArgumentException("관리자 본인 계정은 BLOCKED 처리할 수 없습니다.");
        }

        if ("ROLE_ADMIN".equals(target.getRole()) && "ROLE_USER".equals(role)) {
            long adminCount = userJpaRepository.countByRole("ROLE_ADMIN");
            if (adminCount <= 1) {
                throw new IllegalArgumentException("마지막 관리자 계정은 USER로 변경할 수 없습니다.");
            }
        }

        target.setRole(role);
        target.setStatus(status);
        target.setEnabled("ACTIVE".equals(status));
        return userJpaRepository.save(target);
    }

    public Map<String, Object> toRow(UserEntity u) {
        return Map.of(
                "id", u.getId(),
                "username", u.getLoginId(),
                "name", u.getName(),
                "email", u.getEmail() == null ? "" : u.getEmail(),
                "role", u.getRole(),
                "status", u.getStatus() == null ? (Boolean.TRUE.equals(u.getEnabled()) ? "ACTIVE" : "BLOCKED") : u.getStatus(),
                "createdAt", u.getCreatedAt() == null ? "" : u.getCreatedAt().toString()
        );
    }
}
