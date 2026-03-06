package com.example.lms.controller;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.enrollment.service.RefundService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mypage/payments")
public class PaymentRefundController {

    private final UserJpaRepository userJpaRepository;
    private final RefundService refundService;

    public PaymentRefundController(UserJpaRepository userJpaRepository,
                                   RefundService refundService) {
        this.userJpaRepository = userJpaRepository;
        this.refundService = refundService;
    }

    @PostMapping("/{paymentId}/refund-request")
    public String requestRefund(@PathVariable Long paymentId,
                                Authentication authentication,
                                RedirectAttributes ra) {
        if (authentication == null || !authentication.isAuthenticated()) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }
        Long userId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) {
            ra.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        try {
            refundService.requestRefund(userId, paymentId);
            ra.addFlashAttribute("message", "환불 신청이 접수되었습니다. 관리자 승인 대기 상태입니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/mypage?tab=payments";
    }
}
