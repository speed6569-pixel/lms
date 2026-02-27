package com.example.lms.controller;

import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.MyPageCourseProjection;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/me")
public class EnrollmentQueryController {
    private final UserJpaRepository userJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;

    public EnrollmentQueryController(UserJpaRepository userJpaRepository,
                                     EnrollmentJpaRepository enrollmentJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        Map<String, Object> res = new LinkedHashMap<>();
        if (authentication == null || !authentication.isAuthenticated()) {
            res.put("user", null);
            res.put("summary", Map.of(
                    "attendanceRate", 0.0,
                    "progressRate", 0.0,
                    "hasEnrolledCourses", false,
                    "message", "로그인이 필요합니다."
            ));
            res.put("ongoingCourses", List.of());
            res.put("appliedCourses", List.of());
            return res;
        }

        String loginId = authentication.getName();
        var user = userJpaRepository.findByLoginId(loginId).orElse(null);
        if (user == null) {
            res.put("user", null);
            res.put("summary", Map.of(
                    "attendanceRate", 0.0,
                    "progressRate", 0.0,
                    "hasEnrolledCourses", false,
                    "message", "사용자를 찾을 수 없습니다."
            ));
            res.put("ongoingCourses", List.of());
            res.put("appliedCourses", List.of());
            return res;
        }

        Long userId = user.getId();
        List<MyPageCourseProjection> ongoing = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("APPROVED", "RUNNING"));
        List<MyPageCourseProjection> applied = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("APPLIED", "WAITLIST"));

        boolean hasEnrolled = !ongoing.isEmpty();
        double attendanceRate = hasEnrolled ? safeRate(enrollmentJpaRepository.findAttendanceRate(userId)) : 0.0;
        double progressRate = hasEnrolled ? safeRate(enrollmentJpaRepository.findProgressRate(userId)) : 0.0;

        res.put("user", Map.of(
                "loginId", user.getLoginId(),
                "name", user.getName(),
                "role", user.getRole()
        ));
        res.put("summary", Map.of(
                "attendanceRate", attendanceRate,
                "progressRate", progressRate,
                "hasEnrolledCourses", hasEnrolled,
                "message", hasEnrolled ? "" : "현재 수강 중인 강의가 없습니다."
        ));
        res.put("ongoingCourses", ongoing);
        res.put("appliedCourses", applied);
        return res;
    }

    @GetMapping("/timetable")
    public List<Map<String, Object>> myTimetable(
            @RequestParam(defaultValue = "false") boolean includeRequested,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        String loginId = authentication.getName();
        var user = userJpaRepository.findByLoginId(loginId).orElse(null);
        if (user == null) return List.of();

        Set<String> statuses = includeRequested
                ? Set.of("APPROVED", "RUNNING", "APPLIED", "WAITLIST")
                : Set.of("APPROVED", "RUNNING");

        return enrollmentJpaRepository.findTimetableByStatuses(user.getId(), statuses).stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseCode", v.getCourseCode());
                    m.put("section", v.getSection());
                    m.put("title", v.getTitle());
                    m.put("professor", v.getProfessor());
                    m.put("room", v.getRoom());
                    m.put("day", v.getDay());
                    m.put("startTime", v.getStartTime());
                    m.put("endTime", v.getEndTime());
                    m.put("status", v.getStatus());
                    return m;
                })
                .toList();
    }

    @GetMapping("/enrollments")
    public Map<String, Object> myEnrollments(Authentication authentication) {
        List<Map<String, Object>> lectures = myTimetable(true, authentication);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("count", lectures.size());
        res.put("lectures", lectures);
        return res;
    }

    @GetMapping("/enrollments/history")
    public List<Map<String, Object>> myEnrollmentHistory(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return List.of();
        var user = userJpaRepository.findByLoginId(authentication.getName()).orElse(null);
        if (user == null) return List.of();

        return enrollmentJpaRepository.findMyEnrollmentHistory(user.getId()).stream().map(v -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", v.getEnrollmentId());
            m.put("courseCode", v.getCourseCode());
            m.put("title", v.getTitle());
            m.put("status", v.getStatus());
            m.put("appliedAt", v.getAppliedAt());
            return m;
        }).toList();
    }

    @PostMapping("/enrollments/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelEnrollment(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        var user = userJpaRepository.findByLoginId(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
        }

        var target = enrollmentJpaRepository.findByIdAndUserId(id, user.getId()).orElse(null);
        if (target == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "본인 신청만 취소할 수 있습니다."));
        }

        String next;
        if (Set.of("APPLIED", "WAITLIST").contains(target.getStatus())) next = "CANCELLED";
        else if (Set.of("APPROVED", "RUNNING").contains(target.getStatus())) next = "CANCEL_REQUESTED";
        else return ResponseEntity.badRequest().body(Map.of("success", false, "message", "현재 상태에서는 취소할 수 없습니다."));

        target.setStatus(next);
        enrollmentJpaRepository.save(target);
        return ResponseEntity.ok(Map.of("success", true, "message", "CANCELLED".equals(next) ? "신청이 취소되었습니다." : "취소 요청이 접수되었습니다."));
    }

    private double safeRate(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
    }
}
