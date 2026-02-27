package com.example.lms.controller;

import com.example.lms.enrollment.entity.EnrollmentEntity;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/courses")
public class CourseEnrollmentController {

    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public CourseEnrollmentController(EnrollmentJpaRepository enrollmentJpaRepository,
                                      CourseSessionJpaRepository courseSessionJpaRepository,
                                      UserJpaRepository userJpaRepository) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
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

        if (enrollmentJpaRepository.existsByUserIdAndCourseIdAndStatusIn(userId, courseId, List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"))) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이미 신청한 강의입니다."));
        }

        Long sessionId = courseSessionJpaRepository.findByCourseId(courseId).stream().findFirst().map(s -> s.getId()).orElse(null);
        if (sessionId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "해당 강의의 세션이 없습니다."));
        }

        long current = enrollmentJpaRepository.countByCourseIdAndStatusIn(courseId, List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"));
        int capacity = courseSessionJpaRepository.findByCourseId(courseId).stream().findFirst().map(s -> s.getMaxCount() == null ? 0 : s.getMaxCount()).orElse(0);
        String status = (capacity > 0 && current >= capacity) ? "WAITLIST" : "APPLIED";

        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setCourseSessionId(sessionId);
        entity.setStatus(status);
        entity.setAppliedAt(LocalDateTime.now());
        enrollmentJpaRepository.save(entity);

        return ResponseEntity.ok(Map.of("success", true, "message", "WAITLIST".equals(status) ? "대기 신청되었습니다." : "신청 완료되었습니다."));
    }
}
