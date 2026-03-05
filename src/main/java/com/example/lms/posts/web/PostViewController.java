package com.example.lms.posts.web;

import com.example.lms.posts.service.PostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostViewController {

    private final PostService postService;

    public PostViewController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("title", "공지사항");
        model.addAttribute("posts", postService.userList("SUPPORT"));
        model.addAttribute("category", "SUPPORT");
        model.addAttribute("detailBasePath", "/notices");
        return "posts/posts_list";
    }

    @GetMapping({"/support", "/faqs", "/cs"})
    public String support(Model model) {
        model.addAttribute("title", "FAQ");
        model.addAttribute("posts", postService.userList("CS"));
        model.addAttribute("category", "CS");
        model.addAttribute("detailBasePath", "/support");
        return "posts/posts_list";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        model.addAttribute("title", "교육일정");
        model.addAttribute("posts", postService.userList("SCHEDULE"));
        model.addAttribute("category", "SCHEDULE");
        return "posts/posts_list";
    }

    @GetMapping({"/posts/{id}", "/notices/{id}", "/support/{id}", "/faqs/{id}"})
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("post", postService.get(id));
        return "posts/post_detail";
    }
}
