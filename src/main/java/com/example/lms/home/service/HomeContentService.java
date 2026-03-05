package com.example.lms.home.service;

import com.example.lms.admin.entity.CourseEntity;
import com.example.lms.admin.repo.CourseJpaRepository;
import com.example.lms.home.dto.HomeDtos;
import com.example.lms.posts.entity.PostEntity;
import com.example.lms.posts.repo.PostJpaRepository;
import com.example.lms.support.entity.PostStatus;
import com.example.lms.support.entity.SupportPostEntity;
import com.example.lms.support.repo.SupportPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HomeContentService {

    private final CourseJpaRepository courseJpaRepository;
    private final PostJpaRepository postJpaRepository;
    private final SupportPostRepository supportPostRepository;

    public HomeContentService(CourseJpaRepository courseJpaRepository,
                              PostJpaRepository postJpaRepository,
                              SupportPostRepository supportPostRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.postJpaRepository = postJpaRepository;
        this.supportPostRepository = supportPostRepository;
    }

    @Transactional(readOnly = true)
    public List<HomeDtos.CourseCardDto> latestCourses() {
        List<CourseEntity> courses = courseJpaRepository
                .findTop4ByStatusInAndActiveIsNotAndIsDeletedFalseOrderByCreatedAtDesc(List.of("OPEN", "CLOSED"), false);

        return courses.stream().map(c -> {
            String courseName = firstNonBlank(c.getSubjectName(), c.getTitle(), "제목 없음");
            String summary = firstNonBlank(
                    c.getDescription(),
                    firstNonBlank(c.getSubjectCode(), c.getCourseCode(), "-") + " · "
                            + firstNonBlank(c.getJobGroup(), "직군미지정") + " · "
                            + firstNonBlank(c.getJobLevel(), "직급미지정")
            );
            return new HomeDtos.CourseCardDto(c.getId(), courseName, summary);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<HomeDtos.HomePostSummaryDto> latestNotices() {
        List<PostEntity> posts = postJpaRepository
                .findTop5ByDeletedFalseAndCategoryAndStatusOrderByPinnedDescCreatedAtDesc("SUPPORT", "PUBLISHED");
        return posts.stream()
                .map(p -> new HomeDtos.HomePostSummaryDto(
                        p.getId(),
                        firstNonBlank(p.getTitle(), "제목 없음"),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeDtos.HomeSupportSummaryDto> latestSupportPosts() {
        List<SupportPostEntity> posts = supportPostRepository.findTop5ByOrderByCreatedAtDesc();
        return posts.stream()
                .map(p -> new HomeDtos.HomeSupportSummaryDto(
                        p.getId(),
                        firstNonBlank(p.getTitle(), "제목 없음"),
                        p.getCreatedAt(),
                        p.getStatus() == PostStatus.ANSWERED ? "답변완료" : "대기"
                ))
                .toList();
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
