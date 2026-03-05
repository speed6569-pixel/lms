package com.example.lms.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/user/home")
    public String userHome() {
        return "user/home";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        return adminLayout(model, "관리자 대시보드", "dashboard", "admin/dashboard");
    }

    @GetMapping("/admin/courses")
    public String adminCourses(Model model) {
        return adminLayout(model, "관리자 강의관리", "courses", "admin/admin_courses");
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        return adminLayout(model, "관리자 사용자관리", "users", "admin/admin_users");
    }

    private String adminLayout(Model model, String title, String activeMenu, String contentTemplate) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("contentTemplate", contentTemplate);
        return "admin/layout";
    }
}
