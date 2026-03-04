package com.example.lms.admin.web;

import com.example.lms.enrollment.service.PointService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminPointController {

    private final PointService pointService;

    public AdminPointController(PointService pointService) {
        this.pointService = pointService;
    }

    @PostMapping("/{id}/points/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> grantPoints(@PathVariable Long id,
                                                           @RequestParam(defaultValue = "150000") int amount,
                                                           @RequestParam(required = false) String memo) {
        int balance = pointService.grantPoints(id, amount, memo);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "포인트 지급 완료",
                "granted", amount,
                "balance", balance
        ));
    }
}
