package com.example.lms.controller;

import com.example.lms.course.service.CourseInfoService;
import com.example.lms.enrollment.repo.CourseListProjection;
import com.example.lms.home.service.HomeContentService;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.MyPageCourseProjection;
import com.example.lms.enrollment.repo.MyPointTransactionProjection;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class HomeController {

    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PointTransactionJpaRepository pointTransactionJpaRepository;
    private final CourseInfoService courseInfoService;
    private final HomeContentService homeContentService;

    public HomeController(CourseSessionJpaRepository courseSessionJpaRepository,
                          EnrollmentJpaRepository enrollmentJpaRepository,
                          UserJpaRepository userJpaRepository,
                          PointTransactionJpaRepository pointTransactionJpaRepository,
                          CourseInfoService courseInfoService,
                          HomeContentService homeContentService) {
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
        this.courseInfoService = courseInfoService;
        this.homeContentService = homeContentService;
    }

    @GetMapping({"/", "/homepage"})
    public String home(Model model) {
        model.addAttribute("latestCourses", homeContentService.latestCourses());
        model.addAttribute("latestNotices", homeContentService.latestNotices());
        return "index";
    }

    @GetMapping("/courses")
    public String coursesPage(
            @RequestParam(required = false) String jobGroup,
            @RequestParam(required = false) String jobLevel,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        model.addAttribute("courses", courseInfoService.getCourseInfoRows(keyword, jobGroup, jobLevel));
        model.addAttribute("jobGroup", jobGroup);
        model.addAttribute("jobLevel", jobLevel);
        model.addAttribute("keyword", keyword);
        return "pages/courses";
    }

    @GetMapping("/enrollments/apply")
    public String enrollmentApplyPage(
            @RequestParam(required = false) String job,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String keyword,
            Model model
    ) {
        List<Course> filtered = readCourses().stream()
                .filter(c -> isBlank(job) || c.job().equalsIgnoreCase(job))
                .filter(c -> isBlank(position) || c.position().equalsIgnoreCase(position))
                .filter(c -> isBlank(keyword) || contains(c, keyword))
                .toList();

        model.addAttribute("courses", filtered);
        model.addAttribute("job", job);
        model.addAttribute("position", position);
        model.addAttribute("keyword", keyword);
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

    @GetMapping({"/enrollments/me", "/mypage"})
    public String myPage(
            @RequestParam(required = false, defaultValue = "ongoing") String tab,
            Authentication authentication,
            Model model
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("ongoingCourses", List.of());
            model.addAttribute("appliedCourses", List.of());
            model.addAttribute("closedCourses", List.of());
            model.addAttribute("paymentItems", List.of());
            model.addAttribute("pointBalance", 0);
            model.addAttribute("tab", tab);
            model.addAttribute("attendanceRate", 0.0);
            model.addAttribute("progressRate", 0.0);
            model.addAttribute("hasEnrolledCourses", false);
            model.addAttribute("summaryMessage", "현재 수강 중인 강의가 없습니다.");
            return "pages/my-classroom";
        }

        var user = userJpaRepository.findByLoginId(authentication.getName()).orElse(null);
        if (user == null) {
            model.addAttribute("ongoingCourses", List.of());
            model.addAttribute("appliedCourses", List.of());
            model.addAttribute("closedCourses", List.of());
            model.addAttribute("paymentItems", List.of());
            model.addAttribute("pointBalance", 0);
            model.addAttribute("tab", tab);
            model.addAttribute("attendanceRate", 0.0);
            model.addAttribute("progressRate", 0.0);
            model.addAttribute("hasEnrolledCourses", false);
            model.addAttribute("summaryMessage", "사용자 정보를 찾을 수 없습니다.");
            return "pages/my-classroom";
        }

        Long userId = user.getId();

        List<MyCourseItem> ongoingCourses = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("APPROVED", "RUNNING")).stream()
                .map(this::toMyCourseItem)
                .toList();
        List<MyCourseItem> appliedCourses = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("APPLIED", "WAITLIST")).stream()
                .map(this::toMyCourseItem)
                .toList();
        List<MyCourseItem> closedCourses = enrollmentJpaRepository.findMyCoursesByStatuses(userId, Set.of("REJECTED", "CANCELLED")).stream()
                .map(this::toMyCourseItem)
                .toList();

        List<MyPaymentItem> paymentItems = pointTransactionJpaRepository.findHistoryByUserId(userId).stream()
                .map(this::toMyPointItem)
                .toList();

        boolean hasEnrolledCourses = !ongoingCourses.isEmpty();

        double attendanceRate = 0.0;
        double progressRate = 0.0;
        String summaryMessage = null;

        if (hasEnrolledCourses) {
            attendanceRate = safeRate(enrollmentJpaRepository.findAttendanceRate(userId));
            progressRate = safeRate(enrollmentJpaRepository.findProgressRate(userId));
            if (attendanceRate == 0.0 && progressRate == 0.0) {
                summaryMessage = "출석/진도 데이터가 아직 없습니다.";
            }
        } else {
            summaryMessage = "현재 수강 중인 강의가 없습니다.";
        }

        model.addAttribute("ongoingCourses", ongoingCourses);
        model.addAttribute("appliedCourses", appliedCourses);
        model.addAttribute("closedCourses", closedCourses);
        model.addAttribute("paymentItems", paymentItems);
        model.addAttribute("pointBalance", Optional.ofNullable(user.getPointBalance()).orElse(0));
        model.addAttribute("tab", tab);
        model.addAttribute("attendanceRate", attendanceRate);
        model.addAttribute("progressRate", progressRate);
        model.addAttribute("hasEnrolledCourses", hasEnrolledCourses);
        model.addAttribute("summaryMessage", summaryMessage);
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

    @GetMapping("/customer-center")
    public String customerCenterPage() { return "redirect:/cs"; }

    @GetMapping("/payments")
    public String paymentsPage() { return "redirect:/mypage?tab=payments"; }

    private List<Course> readCourses() {
        List<CourseListProjection> rows = courseSessionJpaRepository.findAllCourseRows();
        Map<String, Integer> enrollCountMap = new HashMap<>();
        courseSessionJpaRepository.findCourseEnrollmentCounts()
                .forEach(v -> enrollCountMap.put(v.getCourseCode(), v.getEnrolledCount() == null ? 0 : v.getEnrolledCount()));

        Map<String, List<CourseListProjection>> grouped = new LinkedHashMap<>();
        for (CourseListProjection r : rows) grouped.computeIfAbsent(r.getCourseCode(), k -> new ArrayList<>()).add(r);

        List<Course> out = new ArrayList<>();
        for (Map.Entry<String, List<CourseListProjection>> e : grouped.entrySet()) {
            List<CourseListProjection> g = e.getValue();
            CourseListProjection first = g.get(0);
            String scheduleText = buildScheduleText(g);
            int maxCount = first.getMaxCount() == null ? 0 : first.getMaxCount();
            int enrolled = enrollCountMap.getOrDefault(first.getCourseCode(), 0);
            boolean open = "OPEN".equalsIgnoreCase(first.getCourseStatus());
            String note = !open ? "모집마감" : (enrolled >= maxCount && maxCount > 0 ? "신청불가" : "신청가능");

            out.add(new Course(
                    0, "", "", "", first.getTitle(), "",
                    maxCount,
                    first.getProfessor(), "", first.getDayNight(),
                    first.getCourseCode(), first.getSection(), scheduleText, note,
                    first.getJob(), first.getPosition(),
                    enrolled, maxCount,
                    first.getPrice(),
                    first.getCourseId(),
                    open
            ));
        }
        return out;
    }

    private String buildScheduleText(List<CourseListProjection> sessions) {
        if (sessions == null || sessions.isEmpty()) return "-";
        List<String> order = List.of("월","화","수","목","금","토","일");
        Map<String, List<String>> timeToDays = new LinkedHashMap<>();
        for (CourseListProjection s : sessions) {
            String time = s.getStartTime() + "~" + s.getEndTime();
            timeToDays.computeIfAbsent(time, k -> new ArrayList<>()).add(s.getDay());
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<String>> en : timeToDays.entrySet()) {
            List<String> days = en.getValue().stream().distinct()
                    .sorted(Comparator.comparingInt(order::indexOf))
                    .toList();
            parts.add(compressDays(days, order) + " " + en.getKey());
        }
        return String.join("; ", parts);
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

    private MyCourseItem toMyCourseItem(MyPageCourseProjection p) {
        return new MyCourseItem(
                p.getTitle(),
                p.getProfessor(),
                p.getClassTime(),
                p.getPrice(),
                p.getCourseCode(),
                p.getSection(),
                p.getEnrolledCount() == null ? 0 : p.getEnrolledCount(),
                p.getMaxCount() == null ? 0 : p.getMaxCount(),
                p.getStatus()
        );
    }

    private MyPaymentItem toMyPointItem(MyPointTransactionProjection p) {
        String status = switch (p.getType()) {
            case "SPEND" -> "PAID";
            case "REFUND" -> "REFUND";
            default -> "EARN";
        };

        String courseLabel = "-";
        if (p.getCourseTitle() != null && !p.getCourseTitle().isBlank()) {
            courseLabel = p.getCourseTitle() + (p.getSubjectCode() == null || p.getSubjectCode().isBlank() ? "" : " / " + p.getSubjectCode());
        }

        return new MyPaymentItem(
                p.getCreatedAt(),
                courseLabel,
                p.getAmount(),
                "POINT",
                status,
                p.getBalanceAfter(),
                p.getMemo()
        );
    }

    private Course findCourse(String courseCode, String section) {
        return readCourses().stream()
                .filter(c -> c.courseCode().equalsIgnoreCase(courseCode) && c.section().equalsIgnoreCase(section))
                .findFirst()
                .orElse(null);
    }

    private double safeRate(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
    }

    private boolean isBlank(String v) { return v == null || v.isBlank(); }

    private boolean contains(Course c, String keyword) {
        String k = keyword.toLowerCase();
        return c.title().toLowerCase().contains(k)
                || c.courseCode().toLowerCase().contains(k)
                || c.professor().toLowerCase().contains(k)
                || c.position().toLowerCase().contains(k)
                || c.job().toLowerCase().contains(k);
    }

    public record MyCourseItem(
            String title,
            String professor,
            String classTime,
            String price,
            String courseCode,
            String section,
            int enrolledCount,
            int maxCount,
            String status
    ) {
    }

    public record MyPaymentItem(
            java.time.LocalDateTime createdAt,
            String courseLabel,
            Integer amount,
            String method,
            String status,
            Integer balanceAfter,
            String memo
    ) {
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
            String price,
            Long courseId,
            boolean open
    ) {
    }
}
