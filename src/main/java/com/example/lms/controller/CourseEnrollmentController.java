package com.example.lms.controller;

import com.example.lms.enrollment.service.CourseEnrollmentService;
import com.example.lms.enrollment.service.InsufficientPointException;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/courses")
public class CourseEnrollmentController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final UserJpaRepository userJpaRepository;

    public CourseEnrollmentController(CourseEnrollmentService courseEnrollmentService,
                                      UserJpaRepository userJpaRepository) {
        this.courseEnrollmentService = courseEnrollmentService;
        this.userJpaRepository = userJpaRepository;
    }

    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<Map<String, Object>> enroll(@PathVariable Long courseId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        Long userId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자 정보를 찾을 수 없습니다."));
        }

        try {
            String status = courseEnrollmentService.enroll(userId, courseId);
            boolean reapplied = status.startsWith("REAPPLIED_");
            String normalized = reapplied ? status.replace("REAPPLIED_", "") : status;
            String message;
            if ("WAITLIST".equals(normalized)) {
                message = reapplied ? "재신청 처리되었습니다. 현재 대기 상태입니다." : "대기 신청되었습니다.";
            } else {
                message = reapplied ? "재신청이 완료되었습니다." : "신청 완료되었습니다.";
            }
            return ResponseEntity.ok(Map.of("success", true, "message", message));
        } catch (InsufficientPointException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
