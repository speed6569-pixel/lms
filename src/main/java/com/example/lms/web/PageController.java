package com.example.lms.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/user/home")
    public String userHome() {
        return "user/home";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/courses")
    public String adminCourses() {
        return "admin/admin_courses";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/admin_users";
    }
}
