package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/courses")
    public String coursesPage(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String openedYear,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Course> filtered = sampleCourses().stream()
                .filter(c -> isBlank(category) || c.category().equalsIgnoreCase(category))
                .filter(c -> isBlank(openedYear) || c.openDate().startsWith(openedYear))
                .filter(c -> isBlank(keyword) || contains(c, keyword))
                .toList();

        model.addAttribute("courses", filtered);
        model.addAttribute("category", category);
        model.addAttribute("openedYear", openedYear);
        model.addAttribute("keyword", keyword);
        return "pages/courses";
    }

    @GetMapping("/enrollments/apply")
    public String enrollmentApplyPage(
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String dayNight,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Course> filtered = sampleCourses().stream()
                .filter(c -> isBlank(job) || c.job().equalsIgnoreCase(job))
                .filter(c -> isBlank(position) || c.position().equalsIgnoreCase(position))
                .filter(c -> isBlank(dayNight) || c.dayNight().equalsIgnoreCase(dayNight))
                .filter(c -> isBlank(keyword) || contains(c, keyword))
                .toList();

        model.addAttribute("courses", filtered);
        model.addAttribute("job", job);
        model.addAttribute("position", position);
        model.addAttribute("dayNight", dayNight);
        model.addAttribute("keyword", keyword);
        model.addAttribute("timeSlots", List.of("09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "19:00"));
        model.addAttribute("weekDays", List.of("월", "화", "수", "목", "금"));
        model.addAttribute("timetableEntries", timetableEntries());
        return "enrollment/apply";
    }

    @PostMapping("/enrollments/apply/request")
    public String requestCourse(
            @RequestParam String courseCode,
            @RequestParam String section,
            RedirectAttributes redirectAttributes
    ) {
        Course target = findCourse(courseCode, section);
        if (target == null) {
            redirectAttributes.addFlashAttribute("message", "신청 실패: 강의를 찾을 수 없습니다.");
            return "redirect:/enrollments/apply";
        }

        if (target.enrolledCount() >= target.maxCount()) {
            redirectAttributes.addFlashAttribute("message", "신청 불가: 정원이 마감된 강의입니다.");
            return "redirect:/enrollments/apply";
        }

        redirectAttributes.addFlashAttribute("message", "신청 요청 완료: " + courseCode + "-" + section);
        return "redirect:/enrollments/apply";
    }

    @GetMapping("/enrollments/me")
    public String myPage(
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String dayNight,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Course> filtered = sampleCourses().stream()
                .filter(c -> isBlank(job) || c.job().equalsIgnoreCase(job))
                .filter(c -> isBlank(dayNight) || c.dayNight().equalsIgnoreCase(dayNight))
                .filter(c -> isBlank(keyword) || contains(c, keyword))
                .toList();

        model.addAttribute("courses", filtered);
        model.addAttribute("job", job);
        model.addAttribute("dayNight", dayNight);
        model.addAttribute("keyword", keyword);
        return "pages/my-classroom";
    }

    @PostMapping("/enrollments/me/request")
    @ResponseBody
    public Map<String, Object> requestCourseAjax(
            @RequestParam String courseCode,
            @RequestParam String section
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (isBlank(courseCode) || isBlank(section)) {
            result.put("success", false);
            result.put("message", "과목코드와 분반을 입력해 주세요.");
            return result;
        }

        Course target = findCourse(courseCode, section);
        if (target == null) {
            result.put("success", false);
            result.put("message", "신청 실패: 강의를 찾을 수 없습니다.");
            return result;
        }

        if (target.enrolledCount() >= target.maxCount()) {
            result.put("success", false);
            result.put("message", "신청 불가: 정원이 마감된 강의입니다.");
            return result;
        }

        result.put("success", true);
        result.put("message", "신청 요청 완료: " + courseCode + "-" + section);
        return result;
    }

    @GetMapping("/support")
    public String learningSupportPage() {
        return "pages/support";
    }

    @GetMapping("/customer-center")
    public String customerCenterPage() {
        return "pages/customer-center";
    }

    @GetMapping("/schedule")
    public String schedulePage() {
        return "pages/schedule";
    }

    private Course findCourse(String courseCode, String section) {
        return sampleCourses().stream()
                .filter(c -> c.courseCode().equalsIgnoreCase(courseCode) && c.section().equalsIgnoreCase(section))
                .findFirst()
                .orElse(null);
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private boolean contains(Course c, String keyword) {
        String k = keyword.toLowerCase();
        return c.title().toLowerCase().contains(k)
                || c.courseCode().toLowerCase().contains(k)
                || c.professor().toLowerCase().contains(k)
                || c.position().toLowerCase().contains(k)
                || c.job().toLowerCase().contains(k);
    }

    private List<TimetableEntry> timetableEntries() {
        return List.of();
    }

    private List<Course> sampleCourses() {
        return List.of();
    }

    public record Course(
            int number,
            String category,
            String operation,
            String process,
            String title,
            String openDate,
            int capacity,
            String professor,
            String division,
            String dayNight,
            String courseCode,
            String section,
            String classTime,
            String note,
            String job,
            String position,
            int enrolledCount,
            int maxCount,
            String price
    ) {
    }

    public record TimetableEntry(
            String courseCode,
            String section,
            String subject,
            String room,
            String day,
            String start,
            String end,
            String category
    ) {
    }
}
