package com.example.lms.controller;

import com.example.lms.enrollment.repo.CourseListProjection;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/enroll")
public class EnrollmentController {

    private static final Map<String, Map<String, EnrollmentItem>> STORE = new ConcurrentHashMap<>();
    private final CourseSessionJpaRepository courseSessionJpaRepository;

    public EnrollmentController(CourseSessionJpaRepository courseSessionJpaRepository) {
        this.courseSessionJpaRepository = courseSessionJpaRepository;
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(
            @RequestParam String courseCode,
            @RequestParam String section,
            HttpSession session
    ) {
        CourseMeta target = findCourse(courseCode, section);
        if (target == null) return ResponseEntity.ok(result(false, "신청 실패: 강의를 찾을 수 없습니다."));
        if (target.enrolledCount() >= target.maxCount()) return ResponseEntity.ok(result(false, "신청 불가: 정원이 마감된 강의입니다."));

        String key = keyOf(courseCode, section);
        Map<String, EnrollmentItem> userMap = STORE.computeIfAbsent(session.getId(), x -> new ConcurrentHashMap<>());
        if (userMap.containsKey(key)) return ResponseEntity.ok(result(true, "이미 신청된 강의입니다."));

        List<CourseMeta> enrolledTargets = userMap.values().stream()
                .map(v -> findCourse(v.courseCode(), v.section()))
                .filter(Objects::nonNull)
                .toList();

        boolean overlapped = enrolledTargets.stream().anyMatch(e -> isTimeOverlapped(e, target));
        if (overlapped) return ResponseEntity.ok(result(false, "신청 불가: 이미 신청한 강의와 시간이 겹칩니다."));

        userMap.put(key, new EnrollmentItem(
                target.courseCode(), target.section(), target.title(), target.professor(), target.classTime(),
                target.day(), target.start(), target.end(), LocalDate.now().toString()
        ));
        return ResponseEntity.ok(result(true, "신청 완료되었습니다"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(
            @RequestParam String courseCode,
            @RequestParam String section,
            HttpSession session
    ) {
        Map<String, EnrollmentItem> userMap = STORE.computeIfAbsent(session.getId(), x -> new ConcurrentHashMap<>());
        userMap.remove(keyOf(courseCode, section));
        return ResponseEntity.ok(result(true, "수강이 취소되었습니다"));
    }

    @GetMapping("/list")
    public ResponseEntity<List<EnrollmentItem>> list(HttpSession session) {
        Map<String, EnrollmentItem> userMap = STORE.computeIfAbsent(session.getId(), x -> new ConcurrentHashMap<>());
        List<EnrollmentItem> items = new ArrayList<>(userMap.values());
        items.sort(Comparator.comparing(EnrollmentItem::date).reversed());
        return ResponseEntity.ok(items);
    }

    private CourseMeta findCourse(String courseCode, String section) {
        return courseSessionJpaRepository.findAllCourseRows().stream()
                .filter(c -> c.getCourseCode().equalsIgnoreCase(courseCode) && c.getSection().equalsIgnoreCase(section))
                .findFirst()
                .map(this::toMeta)
                .orElse(null);
    }

    private CourseMeta toMeta(CourseListProjection p) {
        return new CourseMeta(
                p.getCourseCode(), p.getSection(), p.getTitle(), p.getProfessor(), p.getClassTime(),
                p.getDay(), p.getStartTime(), p.getEndTime(),
                p.getEnrolledCount() == null ? 0 : p.getEnrolledCount(),
                p.getMaxCount() == null ? 0 : p.getMaxCount()
        );
    }

    private String keyOf(String code, String section) { return (code + "-" + section).toUpperCase(); }

    private Map<String, Object> result(boolean success, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", success);
        m.put("message", message);
        return m;
    }

    private boolean isTimeOverlapped(CourseMeta a, CourseMeta b) {
        if (!a.day().equals(b.day())) return false;
        int aStart = parseMinute(a.start());
        int aEnd = parseMinute(a.end());
        int bStart = parseMinute(b.start());
        int bEnd = parseMinute(b.end());
        return aStart < bEnd && bStart < aEnd;
    }

    private int parseMinute(String hhmm) {
        String[] p = hhmm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    record CourseMeta(
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
