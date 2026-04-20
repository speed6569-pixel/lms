package com.example.lms.controller;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.enrollment.service.CourseCardService;
import com.example.lms.enrollment.service.CourseEnrollmentService;
import com.example.lms.enrollment.service.InsufficientPointException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/courses")
public class CourseCardApiController {

    private final UserJpaRepository userJpaRepository;
    private final CourseCardService courseCardService;
    private final CourseEnrollmentService courseEnrollmentService;

    public CourseCardApiController(UserJpaRepository userJpaRepository,
                                   CourseCardService courseCardService,
                                   CourseEnrollmentService courseEnrollmentService) {
        this.userJpaRepository = userJpaRepository;
        this.courseCardService = courseCardService;
        this.courseEnrollmentService = courseEnrollmentService;
    }

    @GetMapping("/{courseId}/card")
    public ResponseEntity<?> card(@PathVariable Long courseId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(courseCardService.getCourseCard(userId, courseId));
    }

    @PostMapping("/{courseId}/pay")
    public ResponseEntity<?> pay(@PathVariable Long courseId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        try {
            return ResponseEntity.ok(courseCardService.payByPoint(userId, courseId));
        } catch (InsufficientPointException e) {
            Map<String, Object> card = courseCardService.getCourseCard(userId, courseId);
            int required = ((Number) card.get("price")).intValue();
            int balance = ((Number) card.get("pointBalance")).intValue();
            return ResponseEntity.status(409).body(Map.of(
                    "code", "INSUFFICIENT_POINTS",
                    "message", "포인트가 부족합니다",
                    "required", required,
                    "balance", balance
            ));
        }
    }

    @PostMapping("/{courseId}/enroll")
    public ResponseEntity<?> enroll(@PathVariable Long courseId, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        try {
            String status = courseEnrollmentService.enroll(userId, courseId);
            String normalized = status.startsWith("REAPPLIED_") ? status.replace("REAPPLIED_", "") : status;
            return ResponseEntity.ok(Map.of("enrolled", true, "status", normalized));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("enrolled", false, "message", e.getMessage()));
        }
    }

    private Long resolveUserId(Authentication authentication) {
        return Optional.ofNullable(authentication)
                .map(Authentication::getName)
                .flatMap(userJpaRepository::findByLoginId)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalArgumentException("로그인이 필요합니다."));
    }
}
