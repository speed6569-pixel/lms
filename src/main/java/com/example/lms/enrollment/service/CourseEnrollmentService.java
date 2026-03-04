package com.example.lms.enrollment.service;

import com.example.lms.admin.entity.CourseEntity;
import com.example.lms.admin.repo.CourseJpaRepository;
import com.example.lms.enrollment.entity.CourseSessionEntity;
import com.example.lms.enrollment.entity.EnrollmentEntity;
import com.example.lms.enrollment.entity.PointTransactionEntity;
import com.example.lms.enrollment.entity.PointTransactionType;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CourseEnrollmentService {

    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final CourseJpaRepository courseJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PointTransactionJpaRepository pointTransactionJpaRepository;

    public CourseEnrollmentService(EnrollmentJpaRepository enrollmentJpaRepository,
                                   CourseSessionJpaRepository courseSessionJpaRepository,
                                   CourseJpaRepository courseJpaRepository,
                                   UserJpaRepository userJpaRepository,
                                   PointTransactionJpaRepository pointTransactionJpaRepository) {
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.courseJpaRepository = courseJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
    }

    @Transactional
    public String enroll(Long userId, Long courseId) {
        UserEntity user = userJpaRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        CourseEntity course = courseJpaRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        if (!"OPEN".equalsIgnoreCase(course.getStatus())) {
            throw new IllegalStateException("모집 마감 강의입니다.");
        }

        if (enrollmentJpaRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("이미 신청 이력이 있는 강의입니다. 중복 신청할 수 없습니다.");
        }

        List<CourseSessionEntity> newSessions = courseSessionJpaRepository.findByCourseId(courseId);
        Long sessionId = newSessions.stream().findFirst().map(CourseSessionEntity::getId).orElse(null);
        if (sessionId == null) throw new IllegalArgumentException("해당 강의의 세션이 없습니다.");

        List<Long> existingCourseIds = enrollmentJpaRepository.findCourseIdsByUserIdAndStatuses(userId,
                List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"));
        if (!existingCourseIds.isEmpty()) {
            List<CourseSessionEntity> existingSessions = courseSessionJpaRepository.findByCourseIdIn(existingCourseIds);
            String conflict = findConflictMessage(existingSessions, newSessions);
            if (conflict != null) throw new IllegalStateException(conflict);
        }

        int price = Optional.ofNullable(course.getPrice()).orElse(0);
        if (price > 0) {
            int currentBalance = safe(user.getPointBalance());
            if (currentBalance < price) {
                throw new InsufficientPointException("포인트가 부족합니다. 현재 잔액: " + currentBalance + "P");
            }

            int nextBalance = currentBalance - price;
            user.setPointBalance(nextBalance);

            PointTransactionEntity spend = new PointTransactionEntity();
            spend.setUserId(userId);
            spend.setCourseId(courseId);
            spend.setType(PointTransactionType.SPEND);
            spend.setAmount(price);
            spend.setBalanceAfter(nextBalance);
            spend.setMemo("강의 결제");
            pointTransactionJpaRepository.save(spend);
        }

        long current = enrollmentJpaRepository.countByCourseIdAndStatusIn(courseId, List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING"));
        int capacity = Optional.ofNullable(course.getCapacity()).orElseGet(() -> newSessions.stream().findFirst().map(s -> s.getMaxCount() == null ? 0 : s.getMaxCount()).orElse(0));
        String status = (capacity > 0 && current >= capacity) ? "WAITLIST" : "APPLIED";

        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setUserId(userId);
        entity.setCourseId(courseId);
        entity.setCourseSessionId(sessionId);
        entity.setStatus(status);
        entity.setAppliedAt(LocalDateTime.now());
        enrollmentJpaRepository.save(entity);

        return status;
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private String findConflictMessage(List<CourseSessionEntity> existingSessions, List<CourseSessionEntity> newSessions) {
        for (CourseSessionEntity n : newSessions) {
            for (CourseSessionEntity e : existingSessions) {
                if (!Objects.equals(nullSafe(n.getDayOfWeek()), nullSafe(e.getDayOfWeek()))) continue;
                if (isOverlapped(e.getStartTime(), e.getEndTime(), n.getStartTime(), n.getEndTime())) {
                    return "시간이 겹쳐 신청할 수 없습니다. "
                            + nullSafe(n.getDayOfWeek()) + " "
                            + formatTime(n.getStartTime()) + "~" + formatTime(n.getEndTime())
                            + " 이 기존 강의와 겹칩니다.";
                }
            }
        }
        return null;
    }

    private boolean isOverlapped(LocalTime existingStart, LocalTime existingEnd, LocalTime newStart, LocalTime newEnd) {
        if (existingStart == null || existingEnd == null || newStart == null || newEnd == null) return false;
        return existingStart.isBefore(newEnd) && newStart.isBefore(existingEnd);
    }

    private String formatTime(LocalTime t) {
        if (t == null) return "--:--";
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    private String nullSafe(String v) {
        return v == null ? "" : v;
    }
}
