package com.example.lms.settings.web;

import com.example.lms.settings.dto.SettingsDtos;
import com.example.lms.settings.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/settings")
    public String settingsPage(@RequestParam(required = false, defaultValue = "profile") String tab,
                               Model model) {
        model.addAttribute("activeTab", tab);
        return "pages/settings";
    }

    @GetMapping("/settings/charge")
    public String chargePage(Model model) {
        model.addAttribute("activeTab", "charge");
        return "pages/settings";
    }

    @PostMapping("/settings/charge")
    public String charge(@RequestParam("chargePlan") int chargePlan,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            int earned = settingsService.chargePoints(authentication.getName(), chargePlan);
            redirectAttributes.addFlashAttribute("flashMessage", "충전 완료: " + earned + "P 적립되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
        }
        return "redirect:/settings/charge";
    }

    @GetMapping("/api/settings/me")
    @ResponseBody
    public SettingsDtos.MeResponse me(Authentication authentication) {
        return settingsService.getMe(authentication.getName());
    }

    @PutMapping("/api/settings/profile")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @RequestBody SettingsDtos.UpdateProfileRequest request,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(settingsService.updateProfile(authentication.getName(), request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/settings/password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody SettingsDtos.ChangePasswordRequest request,
            Authentication authentication
    ) {
        try {
            settingsService.changePassword(authentication.getName(), request);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("message", "비밀번호가 변경되었습니다.");
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/settings/account")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> withdraw(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        settingsService.withdraw(authentication.getName());
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "회원 탈퇴가 완료되었습니다.");
        return ResponseEntity.ok(res);
    }
}
