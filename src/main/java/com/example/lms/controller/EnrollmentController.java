package com.example.lms.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/enroll")
public class EnrollmentController {

    private static final Map<String, Map<String, EnrollmentItem>> STORE = new ConcurrentHashMap<>();

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(
            @RequestParam String courseCode,
            @RequestParam String section,
            HttpSession session
    ) {
        CourseMeta target = courseCatalog().stream()
                .filter(c -> c.courseCode().equalsIgnoreCase(courseCode) && c.section().equalsIgnoreCase(section))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ResponseEntity.ok(result(false, "신청 실패: 강의를 찾을 수 없습니다."));
        }
        if (target.enrolledCount() >= target.maxCount()) {
            return ResponseEntity.ok(result(false, "신청 불가: 정원이 마감된 강의입니다."));
        }

        String key = keyOf(courseCode, section);
        Map<String, EnrollmentItem> userMap = STORE.computeIfAbsent(session.getId(), x -> new ConcurrentHashMap<>());
        if (userMap.containsKey(key)) {
            return ResponseEntity.ok(result(true, "이미 신청된 강의입니다."));
        }

        List<CourseMeta> enrolledTargets = userMap.values().stream()
                .map(v -> findCourse(v.courseCode(), v.section()))
                .filter(Objects::nonNull)
                .toList();

        boolean overlapped = enrolledTargets.stream().anyMatch(e -> isTimeOverlapped(e, target));
        if (overlapped) {
            return ResponseEntity.ok(result(false, "신청 불가: 이미 신청한 강의와 시간이 겹칩니다."));
        }

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

    private String keyOf(String code, String section) {
        return (code + "-" + section).toUpperCase();
    }

    private Map<String, Object> result(boolean success, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", success);
        m.put("message", message);
        return m;
    }

    private CourseMeta findCourse(String courseCode, String section) {
        return courseCatalog().stream()
                .filter(c -> c.courseCode().equalsIgnoreCase(courseCode) && c.section().equalsIgnoreCase(section))
                .findFirst().orElse(null);
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

    private List<CourseMeta> courseCatalog() {
        return List.of(
                new CourseMeta("DEV401", "01", "Spring Boot 실무 API 개발", "김민수", "월 10:00-12:00", "월", "10:00", "12:00", 22, 30),
                new CourseMeta("DEV402", "02", "React 프론트엔드 아키텍처", "이서준", "화 19:00-21:00", "화", "19:00", "21:00", 28, 28),
                new CourseMeta("MKT310", "01", "디지털 퍼널 기획과 전환 최적화", "박지윤", "수 14:00-16:00", "수", "14:00", "16:00", 17, 35),
                new CourseMeta("MNG220", "01", "성과관리와 조직 운영 전략", "최현우", "목 18:30-20:30", "목", "18:30", "20:30", 35, 40),
                new CourseMeta("SAL210", "03", "B2B 제안서 작성과 수주 전략", "정다은", "금 09:30-11:30", "금", "09:30", "11:30", 14, 32),
                new CourseMeta("SAL320", "01", "고객 협상 스킬 부트캠프", "한도윤", "토 13:00-15:00", "토", "13:00", "15:00", 24, 24)
        );
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
    ) {
    }

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
    ) {
    }
}
