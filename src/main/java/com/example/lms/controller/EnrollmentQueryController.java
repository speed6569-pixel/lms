package com.example.lms.controller;

import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.MyPageCourseProjection;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        List<MyPageCourseProjection> ongoing = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("ENROLLED"));
        List<MyPageCourseProjection> applied = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("REQUESTED"));

        boolean hasEnrolled = enrollmentJpaRepository.countEnrolledCourses(userId) > 0;
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
                ? Set.of("ENROLLED", "REQUESTED")
                : Set.of("ENROLLED");

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

    private double safeRate(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
    }
}
