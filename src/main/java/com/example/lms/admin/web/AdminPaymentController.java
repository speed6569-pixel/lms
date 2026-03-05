package com.example.lms.admin.web;

import com.example.lms.admin.service.AdminPaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String qUser,
                       @RequestParam(required = false) String qCourse,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        model.addAttribute("rows", adminPaymentService.search(qUser, qCourse, type, from, to));
        model.addAttribute("qUser", qUser == null ? "" : qUser);
        model.addAttribute("qCourse", qCourse == null ? "" : qCourse);
        model.addAttribute("type", type == null ? "" : type);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return adminLayout(model, "결제 내역 관리", "admin/admin_payments_list :: content");
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("item", adminPaymentService.getDetail(id));
        return adminLayout(model, "결제 내역 상세", "admin/admin_payment_detail :: content");
    }

    private String adminLayout(Model model, String title, String content) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", "payments");
        model.addAttribute("content", content);
        return "admin/layout";
    }
}
