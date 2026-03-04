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

    @GetMapping("/support")
    public String support(Model model) {
        model.addAttribute("title", "공지사항");
        model.addAttribute("posts", postService.userList("SUPPORT"));
        model.addAttribute("category", "SUPPORT");
        return "posts/posts_list";
    }

    @GetMapping("/cs")
    public String cs(Model model) {
        model.addAttribute("title", "고객센터");
        model.addAttribute("posts", postService.userList("CS"));
        model.addAttribute("category", "CS");
        return "posts/posts_list";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        model.addAttribute("title", "교육일정");
        model.addAttribute("posts", postService.userList("SCHEDULE"));
        model.addAttribute("category", "SCHEDULE");
        return "posts/posts_list";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("post", postService.get(id));
        return "posts/post_detail";
    }
}
