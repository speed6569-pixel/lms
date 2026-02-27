package com.example.lms.controller;

import com.example.lms.admin.repo.CourseJpaRepository;
import com.example.lms.enrollment.entity.CourseSessionEntity;
import com.example.lms.enrollment.entity.EnrollmentEntity;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.payments.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/courses")
public class CourseEnrollmentController {

    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final CourseJpaRepository courseJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PaymentService paymentService;

    public CourseEnrollmentController(EnrollmentJpaRepository enrollmentJpaRepository,
                                      CourseSessionJpaRepository courseSessionJpaRepository,
                                      CourseJpaRepository courseJpaRepository,
                                      UserJpaRepository userJpaRepository,
                                      PaymentService paymentService) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.paymentService = paymentService;
    }

    @GetMapping("/{courseId}/preview")
    public ResponseEntity<Map<String, Object>> preview(@PathVariable Long courseId) {
        var course = courseJpaRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "강의를 찾을 수 없습니다."));
        List<CourseSessionEntity> sessions = courseSessionJpaRepository.findByCourseId(courseId);
        String schedule = sessions.stream()
                .sorted(Comparator.comparing(CourseSessionEntity::getDayOfWeek).thenComparing(CourseSessionEntity::getStartTime))
                .map(s -> s.getDayOfWeek() + " " + formatTime(s.getStartTime()) + "~" + formatTime(s.getEndTime()))
                .reduce((a,b) -> a + ", " + b).orElse("-");
        int capacity = Optional.ofNullable(course.getCapacity()).orElse(0);
        long current = enrollmentJpaRepository.countByCourseIdAndStatusIn(courseId, List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"));

        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "subjectCode", Optional.ofNullable(course.getSubjectCode()).orElse(course.getCourseCode()),
                "subjectName", Optional.ofNullable(course.getSubjectName()).orElse(course.getTitle()),
                "instructor", Optional.ofNullable(course.getInstructor()).orElse(course.getProfessor()),
                "scheduleText", schedule,
                "price", Optional.ofNullable(course.getPrice()).orElse(0),
                "current", current,
                "capacity", capacity
        ));
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

        var course = courseJpaRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "강의를 찾을 수 없습니다."));
        }
        if (!"OPEN".equalsIgnoreCase(course.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "모집 마감 강의입니다."));
        }

        if (enrollmentJpaRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이미 신청 이력이 있는 강의입니다. 중복 신청할 수 없습니다."));
        }

        long amount = Optional.ofNullable(course.getPrice()).orElse(0);
        if (amount > 0 && !paymentService.hasPaid(userId, courseId)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "결제 완료 후 신청할 수 있습니다."));
        }

        List<CourseSessionEntity> newSessions = courseSessionJpaRepository.findByCourseId(courseId);
        Long sessionId = newSessions.stream().findFirst().map(CourseSessionEntity::getId).orElse(null);
        if (sessionId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "해당 강의의 세션이 없습니다."));
        }

        List<Long> existingCourseIds = enrollmentJpaRepository.findCourseIdsByUserIdAndStatuses(userId,
                List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"));
        if (!existingCourseIds.isEmpty()) {
            List<CourseSessionEntity> existingSessions = courseSessionJpaRepository.findByCourseIdIn(existingCourseIds);
            String conflict = findConflictMessage(existingSessions, newSessions);
            if (conflict != null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", conflict));
            }
        }

        long current = enrollmentJpaRepository.countByCourseIdAndStatusIn(courseId, List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"));
        int capacity = Optional.ofNullable(course.getCapacity()).orElseGet(() -> newSessions.stream().findFirst().map(s -> s.getMaxCount() == null ? 0 : s.getMaxCount()).orElse(0));
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

    private String findConflictMessage(List<CourseSessionEntity> existingSessions, List<CourseSessionEntity> newSessions) {
        for (CourseSessionEntity n : newSessions) {
            for (CourseSessionEntity e : existingSessions) {
                if (!Objects.equals(nullSafe(n.getDayOfWeek()), nullSafe(e.getDayOfWeek()))) continue;
                if (isOverlapped(e.getStartTime(), e.getEndTime(), n.getStartTime(), n.getEndTime())) {
                    return "시간이 겹쳐 신청할 수 없습니다. "
                            + nullSafe(n.getDayOfWeek()) + " "
                            + formatTime(n.getStartTime()) + "~" + formatTime(n.getEndTime())
                            + " 이 기존 강의와 겹칩니다.";
                }
            }
        }
        return null;
    }

    private boolean isOverlapped(LocalTime existingStart, LocalTime existingEnd, LocalTime newStart, LocalTime newEnd) {
        if (existingStart == null || existingEnd == null || newStart == null || newEnd == null) return false;
        return existingStart.isBefore(newEnd) && newStart.isBefore(existingEnd);
    }

    private String formatTime(LocalTime t) {
        if (t == null) return "--:--";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private String nullSafe(String v) { return v == null ? "" : v; }
}
