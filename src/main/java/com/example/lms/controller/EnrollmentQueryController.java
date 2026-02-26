package com.example.lms.controller;

import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.TimetableLectureProjection;
import com.example.lms.enrollment.repo.UserJpaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class EnrollmentQueryController {
    private final UserJpaRepository userJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;

    public EnrollmentQueryController(UserJpaRepository userJpaRepository,
                                     EnrollmentJpaRepository enrollmentJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
    }

    @GetMapping("/enrollments")
    public Map<String, Object> myEnrollments(HttpSession session) {
        String loginId = (String) session.getAttribute("loginId");
        if (loginId == null || loginId.isBlank()) loginId = "test01";

        var foundUser = userJpaRepository.findByLoginId(loginId).orElse(null);
        Long userId = foundUser == null ? null : foundUser.getId();
        if (userId != null) {
            session.setAttribute("loginId", loginId);
            session.setAttribute("userId", userId);
        }

        List<TimetableLectureProjection> lectures = userId == null
                ? List.of()
                : enrollmentJpaRepository.findEnrolledLectures(userId);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("loginId", loginId);
        res.put("userId", userId);
        res.put("count", lectures.size());
        res.put("lectures", lectures);
        return res;
    }
}
