package com.example.lms.learn.web;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.learn.dto.LearnChatQueryRequest;
import com.example.lms.learn.service.LearnChatService;
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
    private final LearnChatService learnChatService;

    public LearnController(UserJpaRepository userJpaRepository,
                           LearnService learnService,
                           LearnChatService learnChatService) {
        this.userJpaRepository = userJpaRepository;
        this.learnService = learnService;
        this.learnChatService = learnChatService;
    }

    @GetMapping("/learn/{courseId}")
    public String learnPage(@PathVariable Long courseId,
                            Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인이 필요합니다.");
            return "redirect:/login";
        }

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

    @PostMapping("/api/lessons/{lessonId}/progress")
    @ResponseBody
    public ResponseEntity<?> saveProgress(@PathVariable Long lessonId,
                                          @RequestBody Map<String, Object> body,
                                          Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

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

    @PostMapping("/api/learn/{lessonId}/progress")
    @ResponseBody
    public ResponseEntity<?> saveProgressLegacy(@PathVariable Long lessonId,
                                                @RequestBody Map<String, Object> body,
                                                Authentication authentication) {
        return saveProgress(lessonId, body, authentication);
    }

    @PostMapping("/api/learn/{courseId}/chat/query")
    @ResponseBody
    public ResponseEntity<?> queryLearnChat(@PathVariable Long courseId,
                                            @RequestBody LearnChatQueryRequest request,
                                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(Map.of("message", "로그인이 필요합니다."));
        }

        Long userId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) return ResponseEntity.badRequest().body(Map.of("message", "로그인이 필요합니다."));

        try {
            learnService.validateCourseAccess(userId, courseId);
            String mergedContext = mergeRagContextWithPlayback(request);
            String answer = learnChatService.ask(userId, courseId, request.question(), mergedContext);
            return ResponseEntity.ok(Map.of("answer", answer));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "챗봇 응답 생성 중 오류가 발생했습니다."));
        }
    }

    private String mergeRagContextWithPlayback(LearnChatQueryRequest request) {
        String base = request.ragContext() == null ? "" : request.ragContext();

        StringBuilder sb = new StringBuilder(base);
        sb.append("\n\n[재생 컨텍스트]\n")
          .append("- lessonId: ").append(request.lessonId() == null ? "" : request.lessonId()).append("\n")
          .append("- lessonTitle: ").append(request.lessonTitle() == null ? "" : request.lessonTitle()).append("\n")
          .append("- currentTimeSec: ").append(request.currentTimeSec() == null ? 0 : Math.round(request.currentTimeSec())).append("\n")
          .append("- durationSec: ").append(request.durationSec() == null ? 0 : Math.round(request.durationSec())).append("\n")
          .append("- watchedPercent: ").append(request.watchedPercent() == null ? 0 : Math.round(request.watchedPercent())).append("\n")
          .append("- lessonCompleted: ").append(Boolean.TRUE.equals(request.lessonCompleted()) ? "true" : "false").append("\n")
          .append("- courseCompletedLessons: ").append(request.courseCompletedLessons() == null ? 0 : request.courseCompletedLessons()).append("\n")
          .append("- courseTotalLessons: ").append(request.courseTotalLessons() == null ? 0 : request.courseTotalLessons()).append("\n")
          .append("- coursePercent: ").append(request.coursePercent() == null ? 0 : request.coursePercent()).append("\n")
          .append("- 지시: 시간/완료/진도 질문에는 위 재생 컨텍스트 숫자를 우선 근거로 답변하라.");
        return sb.toString();
    }

    @GetMapping("/api/courses/{courseId}/progress")
    @ResponseBody
    public ResponseEntity<?> getCourseProgress(@PathVariable Long courseId,
                                               Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(Map.of("message", "로그인이 필요합니다."));
        }

        Long userId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(null);
        if (userId == null) return ResponseEntity.badRequest().body(Map.of("message", "로그인이 필요합니다."));

        try {
            LearnService.CourseProgress progress = learnService.getCourseProgress(userId, courseId);
            return ResponseEntity.ok(Map.of(
                    "completedLessons", progress.completedLessons(),
                    "totalLessons", progress.totalLessons(),
                    "percent", progress.percent()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
