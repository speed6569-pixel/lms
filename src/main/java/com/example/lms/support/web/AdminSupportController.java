package com.example.lms.support.web;

import com.example.lms.support.service.SupportPostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/support")
public class AdminSupportController {

    private final SupportPostService supportPostService;

    public AdminSupportController(SupportPostService supportPostService) {
        this.supportPostService = supportPostService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posts", supportPostService.getPosts());
        model.addAttribute("title", "고객센터 문의 관리");
        model.addAttribute("activeMenu", "support");
        model.addAttribute("contentTemplate", "admin/admin_support_list");
        return "admin/layout";
    }

    @GetMapping("/{id}/answer")
    public String answerForm(@PathVariable Long id, Model model) {
        model.addAttribute("post", supportPostService.getPost(id));
        model.addAttribute("title", "고객센터 답변");
        model.addAttribute("activeMenu", "support");
        model.addAttribute("contentTemplate", "admin/admin_support_answer");
        return "admin/layout";
    }

    @PostMapping("/{id}/answer")
    public String answer(@PathVariable Long id,
                         @RequestParam String answer,
                         RedirectAttributes ra) {
        supportPostService.answerPost(id, answer);
        ra.addFlashAttribute("message", "답변이 저장되었습니다.");
        return "redirect:/admin/support";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        supportPostService.deletePost(id);
        ra.addFlashAttribute("message", "문의가 삭제되었습니다.");
        return "redirect:/admin/support";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       RedirectAttributes ra) {
        supportPostService.updatePost(id, title, content);
        ra.addFlashAttribute("message", "문의가 수정되었습니다.");
        return "redirect:/admin/support";
    }
}
