package com.example.lms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 가장 기본 화면으로 이동하는 컨트롤러
 * - 처음에는 구조를 이해하기 쉽도록 최소 기능만 둡니다.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // src/main/resources/templates/index.html 렌더링
        return "index";
    }

    @GetMapping("/courses")
    public String coursesPage() {
        return "pages/courses";
    }

    @GetMapping("/enrollments/apply")
    public String enrollmentApplyPage() {
        // src/main/resources/templates/enrollment/apply.html 렌더링
        return "enrollment/apply";
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
}
