package com.example.lms.posts.web;

import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.posts.dto.PostDtos;
import com.example.lms.posts.service.PostService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/posts")
public class AdminPostController {

    private final PostService postService;
    private final UserJpaRepository userJpaRepository;

    public AdminPostController(PostService postService, UserJpaRepository userJpaRepository) {
        this.postService = postService;
        this.userJpaRepository = userJpaRepository;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("posts", postService.adminList(category, q));
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        return adminLayout(model, "게시물 관리", "admin/admin_posts_list :: content");
    }

    @GetMapping("/new")
    public String formNew(Model model) {
        model.addAttribute("mode", "new");
        model.addAttribute("post", null);
        return adminLayout(model, "게시물 등록", "admin/admin_posts_form :: content");
    }

    @GetMapping("/{id}/edit")
    public String formEdit(@PathVariable Long id, Model model) {
        model.addAttribute("mode", "edit");
        model.addAttribute("post", postService.get(id));
        return adminLayout(model, "게시물 수정", "admin/admin_posts_form :: content");
    }

    @PostMapping
    public String create(@ModelAttribute PostDtos.AdminPostSaveRequest req,
                         Authentication authentication,
                         RedirectAttributes ra) {
        Long adminId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(0L);
        postService.create(req, adminId);
        ra.addFlashAttribute("message", "게시물이 등록되었습니다.");
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute PostDtos.AdminPostSaveRequest req,
                         Authentication authentication,
                         RedirectAttributes ra) {
        Long adminId = userJpaRepository.findByLoginId(authentication.getName()).map(u -> u.getId()).orElse(0L);
        postService.update(id, req, adminId);
        ra.addFlashAttribute("message", "게시물이 수정되었습니다.");
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes ra) {
        postService.publish(id);
        ra.addFlashAttribute("message", "게시됨(PUBLISHED) 처리되었습니다.");
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        postService.archive(id);
        ra.addFlashAttribute("message", "보관(ARCHIVED) 처리되었습니다.");
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        postService.softDelete(id);
        ra.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/admin/posts";
    }

    private String adminLayout(Model model, String title, String content) {
        model.addAttribute("title", title);
        model.addAttribute("activeMenu", "posts");
        model.addAttribute("content", content);
        return "admin/layout";
    }
}
