package com.example.lms.admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminCourseContentAliasController {

    @GetMapping("/admin/course-contents")
    public String listAlias() {
        return "redirect:/admin/courses/manage";
    }

    @GetMapping("/admin/course-contents/{courseId}")
    public String detailAlias(@PathVariable Long courseId) {
        return "redirect:/admin/courses/" + courseId + "/manage";
    }
}
