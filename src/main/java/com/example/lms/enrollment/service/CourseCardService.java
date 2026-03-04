package com.example.lms.enrollment.service;

import com.example.lms.admin.entity.CourseEntity;
import com.example.lms.admin.repo.CourseJpaRepository;
import com.example.lms.enrollment.entity.CourseSessionEntity;
import com.example.lms.enrollment.entity.PointTransactionEntity;
import com.example.lms.enrollment.entity.PointTransactionType;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseCardService {

    private final CourseJpaRepository courseJpaRepository;
    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final PointTransactionJpaRepository pointTransactionJpaRepository;

    public CourseCardService(CourseJpaRepository courseJpaRepository,
                             CourseSessionJpaRepository courseSessionJpaRepository,
                             EnrollmentJpaRepository enrollmentJpaRepository,
                             UserJpaRepository userJpaRepository,
                             PointTransactionJpaRepository pointTransactionJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseCard(Long userId, Long courseId) {
        UserEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        CourseEntity course = courseJpaRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        List<CourseSessionEntity> sessions = courseSessionJpaRepository.findByCourseId(courseId).stream()
                .sorted(Comparator.comparing(CourseSessionEntity::getDayOfWeek)
                        .thenComparing(CourseSessionEntity::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        String scheduleText = sessions.stream()
                .map(s -> s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime())
                .collect(Collectors.joining(", "));

        int enrolledCount = (int) enrollmentJpaRepository.countByCourseIdAndStatusIn(
                courseId, List.of("APPLIED", "WAITLIST", "APPROVED", "RUNNING")
        );

        int price = Optional.ofNullable(course.getPrice()).orElse(0);
        int balance = Optional.ofNullable(user.getPointBalance()).orElse(0);
        int netPaid = Optional.ofNullable(pointTransactionJpaRepository.netPaidByUserAndCourse(userId, courseId)).orElse(0);
        boolean paid = (price == 0) || (netPaid >= price);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("courseId", course.getId());
        res.put("courseCode", Objects.requireNonNullElse(course.getSubjectCode(), course.getCourseCode()));
        res.put("title", Objects.requireNonNullElse(course.getSubjectName(), course.getTitle()));
        res.put("professor", Objects.requireNonNullElse(course.getInstructor(), course.getProfessor()));
        res.put("scheduleText", scheduleText);
        res.put("price", price);
        res.put("enrolledCount", enrolledCount);
        res.put("capacity", Optional.ofNullable(course.getCapacity()).orElse(0));
        res.put("room", sessions.stream().map(CourseSessionEntity::getRoom).filter(Objects::nonNull).findFirst().orElse("-"));
        res.put("pointBalance", balance);
        res.put("paid", paid);
        return res;
    }

    @Transactional
    public Map<String, Object> payByPoint(Long userId, Long courseId) {
        UserEntity user = userJpaRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        CourseEntity course = courseJpaRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강의를 찾을 수 없습니다."));

        int price = Optional.ofNullable(course.getPrice()).orElse(0);
        int balance = Optional.ofNullable(user.getPointBalance()).orElse(0);
        if (price <= 0) {
            return Map.of("paid", true, "balance", balance, "message", "무료 강의입니다.");
        }

        int netPaid = Optional.ofNullable(pointTransactionJpaRepository.netPaidByUserAndCourse(userId, courseId)).orElse(0);
        if (netPaid >= price) {
            return Map.of("paid", true, "balance", balance, "message", "이미 결제 완료된 강의입니다.");
        }

        if (balance < price) {
            throw new InsufficientPointException("포인트가 부족합니다.");
        }

        int nextBalance = balance - price;
        user.setPointBalance(nextBalance);

        PointTransactionEntity tx = new PointTransactionEntity();
        tx.setUserId(userId);
        tx.setCourseId(courseId);
        tx.setType(PointTransactionType.SPEND);
        tx.setAmount(price);
        tx.setBalanceAfter(nextBalance);
        tx.setMemo("강의 결제");
        pointTransactionJpaRepository.save(tx);

        return Map.of("paid", true, "balance", nextBalance, "message", "결제 완료");
    }
}
