package com.example.lms.web;

import com.example.lms.enrollment.repo.UserJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalUserModelAdvice {

    private final UserJpaRepository userJpaRepository;

    public GlobalUserModelAdvice(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @ModelAttribute("userName")
    public String userName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        return userJpaRepository.findByLoginId(auth.getName())
                .map(u -> (u.getName() == null || u.getName().isBlank()) ? u.getLoginId() : u.getName())
                .orElse(auth.getName());
    }

    @ModelAttribute("userEmail")
    public String userEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }

    @ModelAttribute("requestUri")
    public String requestUri(HttpServletRequest request) {
        return request == null ? null : request.getRequestURI();
    }
}
