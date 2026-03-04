package com.example.lms.learn.web;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.learn.service.LearnService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class LearnController {

    private final UserJpaRepository userJpaRepository;
    private final LearnService learnService;

    public LearnController(UserJpaRepository userJpaRepository,
                           LearnService learnService) {
        this.userJpaRepository = userJpaRepository;
        this.learnService = learnService;
    }

    @GetMapping("/learn/{courseId}")
    public String learnPage(@PathVariable Long courseId,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Long userId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }

        try {
            var data = learnService.loadPage(userId, courseId);
            model.addAttribute("courseId", data.courseId());
            model.addAttribute("courseName", data.courseName());
            model.addAttribute("courseCode", data.courseCode());
            model.addAttribute("lessons", data.lessons());
            model.addAttribute("progressMap", data.progressMap());
            model.addAttribute("selectedLessonId", data.selectedLessonId());
            return "pages/learn";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/mypage?tab=ongoing";
        }
    }

    @PostMapping("/api/learn/{lessonId}/progress")
    @ResponseBody
    public ResponseEntity<?> saveProgress(@PathVariable Long lessonId,
                                          @RequestBody Map<String, Object> body,
                                          Authentication authentication) {
        Long userId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "로그인이 필요합니다."));

        int progress = body.get("progressPercent") == null ? 0 : ((Number) body.get("progressPercent")).intValue();
        boolean completed = body.get("completed") != null && Boolean.TRUE.equals(body.get("completed"));
        try {
            return ResponseEntity.ok(learnService.saveProgress(userId, lessonId, progress, completed));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
