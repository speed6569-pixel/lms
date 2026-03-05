package com.example.lms.admin.web;

import com.example.lms.admin.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/courses")
public class AdminCourseManageController {

    private final AdminService adminService;

    public AdminCourseManageController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/manage")
    public String manageList(Model model) {
        model.addAttribute("courses", adminService.getCourses());
        return adminLayout(model, "강의 콘텐츠 관리", "admin/admin_course_manage_list");
    }

    @GetMapping("/{courseId}/manage")
    public String manageDetail(@PathVariable Long courseId,
                               @RequestParam(required = false) String q,
                               Model model) {
        model.addAttribute("course", adminService.getCourse(courseId));
        model.addAttribute("lessons", adminService.getLessons(courseId));
        model.addAttribute("learners", adminService.getCourseLearners(courseId, q));
        return adminLayout(model, "강의 상세 관리", "admin/admin_course_manage_detail");
    }

    private String adminLayout(Model model, String title, String contentTemplate) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", "course-content");
        model.addAttribute("contentTemplate", contentTemplate);
        return "admin/layout";
    }
}
