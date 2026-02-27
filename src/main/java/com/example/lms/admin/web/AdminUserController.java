package com.example.lms.admin.web;

import com.example.lms.admin.dto.AdminUserDtos;
import com.example.lms.admin.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(@RequestParam(required = false) String q) {
        return adminUserService.searchUsers(q).stream().map(adminUserService::toRow).toList();
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody AdminUserDtos.UserUpdateRequest req,
                                    Authentication authentication) {
        try {
            return ResponseEntity.ok(adminUserService.toRow(adminUserService.updateUser(id, authentication.getName(), req)));
        } catch (IllegalArgumentException e) {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }
}
