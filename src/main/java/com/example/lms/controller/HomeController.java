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
    public String myPage(
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String dayNight,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Course> filtered = sampleCourses().stream()
                .filter(c -> isBlank(division) || c.division().equalsIgnoreCase(division))
                .filter(c -> isBlank(job) || c.job().equalsIgnoreCase(job))
                .filter(c -> isBlank(dayNight) || c.dayNight().equalsIgnoreCase(dayNight))
                .filter(c -> isBlank(keyword) || contains(c, keyword))
                .toList();

        model.addAttribute("courses", filtered);
        model.addAttribute("division", division);
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
                new Course(1, "전공", "온라인", "개발트랙", "Spring Boot 실무 API 개발", "2026-03-10", 30, "김민수", "전공", "주간", 2, "DEV401", "01", 3, "월 10:00-12:00", "정원 30", "신청가능", "개발", "대리"),
                new Course(2, "전공", "오프라인", "개발트랙", "React 프론트엔드 아키텍처", "2026-03-12", 28, "이서준", "전공", "야간", 3, "DEV402", "02", 3, "화 19:00-21:00", "정원 28", "신청가능", "개발", "과장"),
                new Course(3, "기타", "온라인", "기획/마케팅트랙", "디지털 퍼널 기획과 전환 최적화", "2026-03-11", 35, "박지윤", "교양", "주간", 2, "MKT310", "01", 2, "수 14:00-16:00", "정원 35", "신청가능", "기획/마케팅", "주임"),
                new Course(4, "기타", "오프라인", "경영트랙", "성과관리와 조직 운영 전략", "2026-03-14", 40, "최현우", "교양", "야간", 3, "MNG220", "01", 2, "목 18:30-20:30", "정원 40", "신청가능", "경영", "차장"),
                new Course(5, "전공", "온라인", "영업트랙", "B2B 제안서 작성과 수주 전략", "2026-03-15", 32, "정다은", "전공", "주간", 2, "SAL210", "03", 2, "금 09:30-11:30", "정원 32", "신청가능", "영업", "사원"),
                new Course(6, "기타", "오프라인", "영업트랙", "고객 협상 스킬 부트캠프", "2026-03-18", 24, "한도윤", "교양", "야간", 2, "SAL320", "01", 2, "토 13:00-15:00", "정원 24", "신청가능", "영업", "부장")
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
