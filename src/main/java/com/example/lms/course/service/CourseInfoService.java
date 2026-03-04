package com.example.lms.course.service;

import com.example.lms.course.dto.CourseInfoRowDto;
import com.example.lms.enrollment.repo.CourseListProjection;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseInfoService {

    private final CourseSessionJpaRepository courseSessionJpaRepository;

    public CourseInfoService(CourseSessionJpaRepository courseSessionJpaRepository) {
        this.courseSessionJpaRepository = courseSessionJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseInfoRowDto> getCourseInfoRows(String keyword, String jobGroup, String jobLevel) {
        List<CourseListProjection> rows = courseSessionJpaRepository.findAllCourseRows();

        Map<Long, List<CourseListProjection>> grouped = new LinkedHashMap<>();
        for (CourseListProjection r : rows) {
            if (!"OPEN".equalsIgnoreCase(r.getCourseStatus()) && !"CLOSED".equalsIgnoreCase(r.getCourseStatus())) continue;
            grouped.computeIfAbsent(r.getCourseId(), k -> new ArrayList<>()).add(r);
        }

        return grouped.values().stream()
                .map(this::toDto)
                .filter(v -> isBlank(jobGroup) || equalsIgnoreCase(v.jobGroup(), jobGroup))
                .filter(v -> isBlank(jobLevel) || equalsIgnoreCase(v.jobLevel(), jobLevel))
                .filter(v -> isBlank(keyword) || contains(v, keyword))
                .toList();
    }

    private CourseInfoRowDto toDto(List<CourseListProjection> sessions) {
        CourseListProjection first = sessions.get(0);
        return new CourseInfoRowDto(
                emptyToDash(first.getCourseCode()),
                emptyToDash(first.getJob()),
                emptyToDash(first.getPosition()),
                emptyToDash(first.getTitle()),
                emptyToDash(first.getProfessor()),
                buildScheduleText(sessions),
                emptyToDash(first.getPrice()),
                first.getMaxCount() == null ? 0 : first.getMaxCount(),
                emptyToDash(first.getCreatedAt())
        );
    }

    private String buildScheduleText(List<CourseListProjection> sessions) {
        if (sessions == null || sessions.isEmpty()) return "-";

        List<String> order = List.of("월", "화", "수", "목", "금", "토", "일");
        Map<String, List<String>> timeToDays = new LinkedHashMap<>();

        sessions.stream()
                .sorted(Comparator.comparing(CourseListProjection::getStartTime, Comparator.nullsLast(String::compareTo)))
                .forEach(s -> {
                    String time = s.getStartTime() + "~" + s.getEndTime();
                    timeToDays.computeIfAbsent(time, k -> new ArrayList<>()).add(s.getDay());
                });

        List<String> parts = new ArrayList<>();
        for (var en : timeToDays.entrySet()) {
            List<String> days = en.getValue().stream().distinct()
                    .sorted(Comparator.comparingInt(order::indexOf))
                    .toList();
            parts.add(compressDays(days, order) + " " + en.getKey());
        }
        return String.join(", ", parts);
    }

    private String compressDays(List<String> days, List<String> order) {
        if (days.isEmpty()) return "";
        if (days.size() == 1) return days.get(0);

        List<String> chunks = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= days.size(); i++) {
            boolean broken = (i == days.size()) || (order.indexOf(days.get(i)) - order.indexOf(days.get(i - 1)) != 1);
            if (broken) {
                String from = days.get(start);
                String to = days.get(i - 1);
                if (start == i - 1) chunks.add(from);
                else if (i - start == 2) chunks.add(from + "/" + to);
                else chunks.add(from + "~" + to);
                start = i;
            }
        }
        return String.join("/", chunks);
    }

    private boolean contains(CourseInfoRowDto v, String keyword) {
        String k = keyword.toLowerCase();
        return v.courseName().toLowerCase().contains(k)
                || v.instructor().toLowerCase().contains(k)
                || v.subjectCode().toLowerCase().contains(k);
    }

    private boolean isBlank(String v) { return v == null || v.isBlank(); }
    private boolean equalsIgnoreCase(String a, String b) { return a != null && a.equalsIgnoreCase(b); }
    private String emptyToDash(String v) { return isBlank(v) ? "-" : v; }
}
