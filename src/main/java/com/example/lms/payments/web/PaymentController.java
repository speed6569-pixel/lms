package com.example.lms.payments.web;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.payments.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserJpaRepository userJpaRepository;

    public PaymentController(PaymentService paymentService, UserJpaRepository userJpaRepository) {
        this.paymentService = paymentService;
        this.userJpaRepository = userJpaRepository;
    }

    @PostMapping("/prepare")
    public ResponseEntity<?> prepare(@RequestBody Map<String, Object> req, Authentication auth) {
        Long userId = userJpaRepository.findByLoginId(auth.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
        try {
            Long courseId = Long.valueOf(String.valueOf(req.get("courseId")));
            String provider = String.valueOf(req.getOrDefault("provider", "CARD"));
            return ResponseEntity.ok(paymentService.prepare(userId, courseId, provider));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody Map<String, Object> req, Authentication auth) {
        Long userId = userJpaRepository.findByLoginId(auth.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "사용자를 찾을 수 없습니다."));
        try {
            Long paymentId = Long.valueOf(String.valueOf(req.get("paymentId")));
            String tx = String.valueOf(req.getOrDefault("providerTxId", ""));
            return ResponseEntity.ok(paymentService.confirm(userId, paymentId, tx));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
