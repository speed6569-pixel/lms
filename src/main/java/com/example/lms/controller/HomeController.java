package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String dayNight,
            Model model
    ) {
        List<Course> filtered = sampleCourses().stream()
                .filter(c -> isBlank(division) || c.division().equalsIgnoreCase(division))
                .filter(c -> isBlank(job) || c.job().equalsIgnoreCase(job))
                .filter(c -> isBlank(position) || c.position().equalsIgnoreCase(position))
                .filter(c -> isBlank(dayNight) || c.dayNight().equalsIgnoreCase(dayNight))
                .toList();

        model.addAttribute("courses", filtered);
        model.addAttribute("division", division);
        model.addAttribute("job", job);
        model.addAttribute("position", position);
        model.addAttribute("dayNight", dayNight);
        return "enrollment/apply";
    }

    @PostMapping("/enrollments/apply/request")
    public String requestCourse(
            @RequestParam String courseCode,
            @RequestParam String section,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("message", "신청 요청 완료: " + courseCode + "-" + section);
        return "redirect:/enrollments/apply";
    }

    @GetMapping("/enrollments/me")
    public String myClassroomPage() {
        return "pages/my-classroom";
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

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private boolean contains(Course c, String keyword) {
        String k = keyword.toLowerCase();
        return c.title().toLowerCase().contains(k)
                || c.courseCode().toLowerCase().contains(k)
                || c.professor().toLowerCase().contains(k)
                || c.position().toLowerCase().contains(k);
    }

    private List<Course> sampleCourses() {
        return List.of(
                new Course(1, "전공", "공학", "컴퓨터공학", "웹프로그래밍", "2026-03-01", 40, "김교수", "전공", "주간", 2, "CSE201", "01", 3, "월 10:00-12:00", "정원 40", "신청가능"),
                new Course(2, "전공", "공학", "컴퓨터공학", "데이터베이스", "2026-03-01", 35, "이교수", "전공", "주간", 3, "CSE301", "02", 3, "화 13:00-15:00", "정원 35", "신청가능"),
                new Course(3, "기타", "교양", "공통", "커뮤니케이션 스킬", "2026-03-02", 60, "박교수", "교양", "야간", 1, "GEN101", "01", 2, "수 19:00-21:00", "정원 60", "잔여적음"),
                new Course(4, "전공", "공학", "소프트웨어", "알고리즘", "2025-09-01", 30, "최교수", "전공", "주간", 2, "SWE220", "01", 3, "목 09:00-11:00", "정원 30", "신청가능")
        );
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
            int grade,
            String courseCode,
            String section,
            int credits,
            String classTime,
            String limitText,
            String note,
            String job,
            String position
    ) {
        public Course(int number, String category, String operation, String process, String title, String openDate,
                      int capacity, String professor, String division, String dayNight, int grade, String courseCode,
                      String section, int credits, String classTime, String limitText, String note) {
            this(number, category, operation, process, title, openDate, capacity, professor, division, dayNight,
                    grade, courseCode, section, credits, classTime, limitText, note, operation, process);
        }
    }
}
