package com.example.lms.learn.service;

import com.example.lms.admin.entity.CourseEntity;
import com.example.lms.admin.repo.CourseJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.learn.entity.LessonEntity;
import com.example.lms.learn.entity.LessonProgressEntity;
import com.example.lms.learn.repo.LessonJpaRepository;
import com.example.lms.learn.repo.LessonProgressJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class LearnService {

    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final CourseJpaRepository courseJpaRepository;
    private final LessonJpaRepository lessonJpaRepository;
    private final LessonProgressJpaRepository lessonProgressJpaRepository;

    public LearnService(EnrollmentJpaRepository enrollmentJpaRepository,
                        CourseJpaRepository courseJpaRepository,
                        LessonJpaRepository lessonJpaRepository,
                        LessonProgressJpaRepository lessonProgressJpaRepository) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
        this.lessonJpaRepository = lessonJpaRepository;
        this.lessonProgressJpaRepository = lessonProgressJpaRepository;
    }

    @Transactional(readOnly = true)
    public LearnPageData loadPage(Long userId, Long courseId) {
        if (!enrollmentJpaRepository.existsByUserIdAndCourseIdAndStatusIn(userId, courseId, List.of("APPROVED", "RUNNING"))) {
            throw new IllegalStateException("승인 후 수강 가능합니다.");
        }

        CourseEntity course = courseJpaRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        List<LessonEntity> lessons = lessonJpaRepository.findByCourseIdOrderByOrderNoAsc(courseId);
        List<Long> ids = lessons.stream().map(LessonEntity::getId).toList();

        Map<Long, LessonProgressEntity> progressMap = new HashMap<>();
        if (!ids.isEmpty()) {
            lessonProgressJpaRepository.findByUserIdAndLessonIdIn(userId, ids)
                    .forEach(p -> progressMap.put(p.getLessonId(), p));
        }

        Long selectedLessonId = lessons.stream().findFirst().map(LessonEntity::getId).orElse(null);
        Optional<LessonProgressEntity> latest = lessonProgressJpaRepository.findLatestByUserAndCourse(userId, courseId);
        if (latest.isPresent()) selectedLessonId = latest.get().getLessonId();

        String courseName = (course.getSubjectName() != null && !course.getSubjectName().isBlank()) ? course.getSubjectName() : course.getTitle();
        String courseCode = (course.getSubjectCode() != null && !course.getSubjectCode().isBlank()) ? course.getSubjectCode() : course.getCourseCode();

        return new LearnPageData(courseId, courseName, courseCode, lessons, progressMap, selectedLessonId);
    }

    @Transactional
    public Map<String, Object> saveProgress(Long userId, Long lessonId, int progressPercent, boolean completed) {
        LessonEntity lesson = lessonJpaRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("차시를 찾을 수 없습니다."));

        if (!enrollmentJpaRepository.existsByUserIdAndCourseIdAndStatusIn(userId, lesson.getCourseId(), List.of("APPROVED", "RUNNING"))) {
            throw new IllegalStateException("승인 후 수강 가능합니다.");
        }

        LessonProgressEntity p = lessonProgressJpaRepository.findByUserIdAndLessonId(userId, lessonId).orElseGet(() -> {
            LessonProgressEntity n = new LessonProgressEntity();
            n.setUserId(userId);
            n.setLessonId(lessonId);
            return n;
        });

        int clamped = Math.max(0, Math.min(100, progressPercent));
        p.setProgressPercent(clamped);
        p.setCompleted(completed || clamped >= 90);
        lessonProgressJpaRepository.save(p);

        CourseProgress courseProgress = getCourseProgress(userId, lesson.getCourseId());

        return Map.of(
                "success", true,
                "progressPercent", p.getProgressPercent(),
                "completed", p.getCompleted(),
                "courseId", lesson.getCourseId(),
                "courseProgress", courseProgress
        );
    }

    @Transactional(readOnly = true)
    public void validateCourseAccess(Long userId, Long courseId) {
        if (!enrollmentJpaRepository.existsByUserIdAndCourseIdAndStatusIn(userId, courseId, List.of("APPROVED", "RUNNING"))) {
            throw new IllegalStateException("승인 후 수강 가능합니다.");
        }
    }

    @Transactional(readOnly = true)
    public CourseProgress getCourseProgress(Long userId, Long courseId) {
        validateCourseAccess(userId, courseId);

        long totalLessons = lessonJpaRepository.countByCourseId(courseId);
        if (totalLessons <= 0) {
            return new CourseProgress(0L, 0L, 0);
        }

        long completedLessons = lessonProgressJpaRepository.countCompletedByUserIdAndCourseId(userId, courseId);
        int percent = (int) Math.max(0, Math.min(100, Math.round((completedLessons * 100.0) / totalLessons)));
        return new CourseProgress(completedLessons, totalLessons, percent);
    }

    public record LearnPageData(
            Long courseId,
            String courseName,
            String courseCode,
            List<LessonEntity> lessons,
            Map<Long, LessonProgressEntity> progressMap,
            Long selectedLessonId
    ) {}

    public record CourseProgress(
            Long completedLessons,
            Long totalLessons,
            Integer percent
    ) {}
}
