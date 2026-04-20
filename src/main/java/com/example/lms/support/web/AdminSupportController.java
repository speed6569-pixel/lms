package com.example.lms.support.web;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.support.service.SupportPostService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/support")
public class AdminSupportController {

    private final SupportPostService supportPostService;
    private final UserJpaRepository userJpaRepository;

    public AdminSupportController(SupportPostService supportPostService,
                                  UserJpaRepository userJpaRepository) {
        this.supportPostService = supportPostService;
        this.userJpaRepository = userJpaRepository;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        requireAdmin(authentication);
        model.addAttribute("posts", supportPostService.getPostsForAdmin());
        model.addAttribute("title", "고객센터 문의 관리");
        model.addAttribute("activeMenu", "support");
        model.addAttribute("contentTemplate", "admin/admin_support_list");
        return "admin/layout";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        requireAdmin(authentication);
        model.addAttribute("post", supportPostService.getPost(id));
        model.addAttribute("title", "고객센터 답변");
        model.addAttribute("activeMenu", "support");
        model.addAttribute("contentTemplate", "admin/admin_support_answer");
        return "admin/layout";
    }

    @PostMapping("/{id}/answer")
    public String answer(@PathVariable Long id,
                         @RequestParam String answer,
                         Authentication authentication,
                         RedirectAttributes ra) {
        requireAdmin(authentication);
        supportPostService.answerPost(id, answer);
        ra.addFlashAttribute("message", "답변이 저장되었습니다.");
        return "redirect:/admin/support";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes ra) {
        requireAdmin(authentication);
        supportPostService.deletePost(id);
        ra.addFlashAttribute("message", "문의가 삭제되었습니다.");
        return "redirect:/admin/support";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       Authentication authentication,
                       RedirectAttributes ra) {
        requireAdmin(authentication);
        supportPostService.updatePost(id, title, content);
        ra.addFlashAttribute("message", "문의가 수정되었습니다.");
        return "redirect:/admin/support";
    }

    private void requireAdmin(Authentication authentication) {
        boolean byAuthority = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));

        if (byAuthority) return;

        var user = userJpaRepository.findByLoginId(authentication.getName()).orElse(null);
        boolean byDbRole = user != null && user.getRole() != null && user.getRole().toUpperCase().contains("ADMIN");
        if (!byDbRole) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }
    }
}
