package com.example.lms.admin.web;

import com.example.lms.admin.service.AdminPaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        return adminLayout(model, "결제 내역 관리", "admin/admin_payments_list");
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("view", adminPaymentService.getDetail(id));
        return adminLayout(model, "결제 내역 상세", "admin/admin_payment_detail");
    }

    @PostMapping("/{id}/refund/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        try {
            adminPaymentService.approveRefund(id);
            ra.addFlashAttribute("message", "환불 승인 처리되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    @PostMapping("/{id}/refund/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        try {
            adminPaymentService.rejectRefund(id, reason);
            ra.addFlashAttribute("message", "환불 거절 처리되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/payments/" + id;
    }

    private String adminLayout(Model model, String title, String contentTemplate) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", "payments");
        model.addAttribute("contentTemplate", contentTemplate);
        return "admin/layout";
    }
}
