package com.example.lms.controller;

import com.example.lms.enrollment.entity.EnrollmentEntity;
import com.example.lms.enrollment.repo.CourseListProjection;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.TimetableLectureProjection;
import com.example.lms.enrollment.repo.UserJpaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/enroll")
public class EnrollmentController {

    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public EnrollmentController(CourseSessionJpaRepository courseSessionJpaRepository,
                                EnrollmentJpaRepository enrollmentJpaRepository,
                                UserJpaRepository userJpaRepository) {
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(
            @RequestParam String courseCode,
            @RequestParam String section,
            HttpSession session
    ) {
        Long userId = resolveUserId(session);
        if (userId == null) return ResponseEntity.ok(result(false, "로그인 사용자 정보를 찾을 수 없습니다."));

        CourseMeta target = findCourse(courseCode, section);
        if (target == null) return ResponseEntity.ok(result(false, "신청 실패: 강의를 찾을 수 없습니다."));
        if (target.enrolledCount() >= target.maxCount()) return ResponseEntity.ok(result(false, "신청 불가: 정원이 마감된 강의입니다."));

        if (enrollmentJpaRepository.existsByUserIdAndCourseSessionIdAndStatus(userId, target.sessionId(), "ENROLLED")) {
            return ResponseEntity.ok(result(true, "이미 신청된 강의입니다."));
        }

        boolean overlapped = enrollmentJpaRepository.findEnrolledLectures(userId).stream()
                .anyMatch(e -> isTimeOverlapped(e, target));
        if (overlapped) return ResponseEntity.ok(result(false, "신청 불가: 이미 신청한 강의와 시간이 겹칩니다."));

        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setUserId(userId);
        entity.setCourseSessionId(target.sessionId());
        entity.setStatus("ENROLLED");
        entity.setEnrolledAt(LocalDateTime.now());
        enrollmentJpaRepository.save(entity);

        return ResponseEntity.ok(result(true, "신청 완료되었습니다"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @RequestParam String courseCode,
            @RequestParam String section,
            HttpSession session
    ) {
        Long userId = resolveUserId(session);
        if (userId == null) return ResponseEntity.ok(result(false, "로그인 사용자 정보를 찾을 수 없습니다."));

        CourseMeta target = findCourse(courseCode, section);
        if (target == null) return ResponseEntity.ok(result(false, "취소 실패: 강의를 찾을 수 없습니다."));

        EnrollmentEntity current = enrollmentJpaRepository
                .findTopByUserIdAndCourseSessionIdAndStatusOrderByIdDesc(userId, target.sessionId(), "ENROLLED");
        if (current != null) {
            current.setStatus("CANCELED");
            current.setCanceledAt(LocalDateTime.now());
            enrollmentJpaRepository.save(current);
        }

        return ResponseEntity.ok(result(true, "수강이 취소되었습니다"));
    }

    @GetMapping("/list")
    public ResponseEntity<List<EnrollmentItem>> list(HttpSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) return ResponseEntity.ok(List.of());

        List<EnrollmentItem> items = enrollmentJpaRepository.findEnrolledLectures(userId).stream()
                .map(v -> new EnrollmentItem(
                        v.getCourseCode(),
                        v.getSection(),
                        v.getTitle(),
                        v.getProfessor(),
                        v.getDay() + " " + v.getStartTime() + "-" + v.getEndTime(),
                        v.getDay(),
                        v.getStartTime(),
                        v.getEndTime(),
                        LocalDate.now().toString()
                ))
                .toList();

        return ResponseEntity.ok(items);
    }

    private Long resolveUserId(HttpSession session) {
        String loginId = (String) session.getAttribute("loginId");
        if (loginId == null || loginId.isBlank()) loginId = "test01";

        var user = userJpaRepository.findByLoginId(loginId).orElse(null);
        if (user != null) {
            session.setAttribute("loginId", loginId);
            session.setAttribute("userId", user.getId());
            return user.getId();
        }
        return null;
    }

    private CourseMeta findCourse(String courseCode, String section) {
        return courseSessionJpaRepository.findAllCourseRows().stream()
                .filter(c -> c.getCourseCode().equalsIgnoreCase(courseCode)
                        && (section == null || section.isBlank() || c.getSection().equalsIgnoreCase(section)))
                .findFirst()
                .map(this::toMeta)
                .orElse(null);
    }

    private CourseMeta toMeta(CourseListProjection p) {
        return new CourseMeta(
                p.getSessionId(),
                p.getCourseCode(),
                p.getSection(),
                p.getTitle(),
                p.getProfessor(),
                p.getClassTime(),
                p.getDay(),
                p.getStartTime(),
                p.getEndTime(),
                p.getEnrolledCount() == null ? 0 : p.getEnrolledCount(),
                p.getMaxCount() == null ? 0 : p.getMaxCount()
        );
    }

    private Map<String, Object> result(boolean success, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", success);
        m.put("message", message);
        return m;
    }

    private boolean isTimeOverlapped(TimetableLectureProjection a, CourseMeta b) {
        if (!a.getDay().equals(b.day())) return false;
        int aStart = parseMinute(a.getStartTime());
        int aEnd = parseMinute(a.getEndTime());
        int bStart = parseMinute(b.start());
        int bEnd = parseMinute(b.end());
        return aStart < bEnd && bStart < aEnd;
    }

    private int parseMinute(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    record CourseMeta(
            Long sessionId,
            String courseCode,
            String section,
            String title,
            String professor,
            String classTime,
            String day,
            String start,
            String end,
            int enrolledCount,
            int maxCount
    ) { }

    public record EnrollmentItem(
            String courseCode,
            String section,
            String title,
            String professor,
            String classTime,
            String day,
            String start,
            String end,
            String date
    ) { }
}
