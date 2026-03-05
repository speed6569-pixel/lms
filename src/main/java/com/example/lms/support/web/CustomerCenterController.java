package com.example.lms.support.web;

import com.example.lms.support.service.SupportPostService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer-center")
public class CustomerCenterController {

    private final SupportPostService supportPostService;

    public CustomerCenterController(SupportPostService supportPostService) {
        this.supportPostService = supportPostService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        String writer = authentication.getName();
        model.addAttribute("posts", supportPostService.getPostsByWriter(writer));
        return "support/customer-center";
    }

    @GetMapping("/write")
    public String writeForm() {
        return "support/customer-center-write";
    }

    @PostMapping
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        Authentication authentication,
                        RedirectAttributes ra) {
        supportPostService.createPost(title, content, authentication.getName());
        ra.addFlashAttribute("message", "문의가 등록되었습니다.");
        return "redirect:/customer-center";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        var post = supportPostService.getPost(id);
        if (!authentication.getName().equals(post.getWriter())) {
            throw new IllegalArgumentException("본인이 작성한 문의만 조회할 수 있습니다.");
        }
        model.addAttribute("post", post);
        return "support/customer-center-detail";
    }
}
