package com.example.lms.admin.service;

import com.example.lms.admin.dto.AdminDtos;
import com.example.lms.admin.entity.*;
import com.example.lms.admin.repo.*;
import com.example.lms.enrollment.entity.CourseSessionEntity;
import com.example.lms.enrollment.entity.EnrollmentEntity;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

@Service
public class AdminService {
    private final CourseJpaRepository courseJpaRepository;
    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final AttendanceRecordJpaRepository attendanceRecordJpaRepository;
    private final ProgressRecordJpaRepository progressRecordJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AuditLogJpaRepository auditLogJpaRepository;

    public AdminService(CourseJpaRepository courseJpaRepository,
                        CourseSessionJpaRepository courseSessionJpaRepository,
                        EnrollmentJpaRepository enrollmentJpaRepository,
                        AttendanceRecordJpaRepository attendanceRecordJpaRepository,
                        ProgressRecordJpaRepository progressRecordJpaRepository,
                        UserJpaRepository userJpaRepository,
                        AuditLogJpaRepository auditLogJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.attendanceRecordJpaRepository = attendanceRecordJpaRepository;
        this.progressRecordJpaRepository = progressRecordJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    @Transactional
    public CourseEntity createCourse(AdminDtos.CourseCreateRequest req, Long adminUserId, String ip) {
        if (req.courseCode() == null || req.courseCode().isBlank()) throw new IllegalArgumentException("과정코드는 필수입니다.");
        if (courseJpaRepository.existsByCourseCode(req.courseCode().trim())) throw new IllegalArgumentException("이미 존재하는 과정코드입니다.");
        if (req.price() != null && req.price() < 0) throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        if (req.maxCount() != null && req.maxCount() < 0) throw new IllegalArgumentException("정원은 0 이상이어야 합니다.");

        CourseEntity c = new CourseEntity();
        c.setCourseCode(req.courseCode().trim());
        c.setTitle(req.title());
        c.setDescription(req.description());
        c.setProfessor(req.professor());
        c.setPrice(req.price() == null ? 0 : req.price());
        c.setMaxCount(req.maxCount() == null ? 0 : req.maxCount());
        c.setClassTime(req.classTime());
        c.setActive(true);
        CourseEntity saved = courseJpaRepository.save(c);
        audit(adminUserId, "CREATE_COURSE", "COURSE", saved.getId(), ip, req.toString());
        return saved;
    }

    @Transactional
    public CourseEntity updateCourse(Long id, AdminDtos.CourseUpdateRequest req, Long adminUserId, String ip) {
        CourseEntity c = courseJpaRepository.findById(id).orElseThrow();
        if (req.title() != null) c.setTitle(req.title());
        if (req.description() != null) c.setDescription(req.description());
        if (req.professor() != null) c.setProfessor(req.professor());
        if (req.price() != null) {
            if (req.price() < 0) throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
            c.setPrice(req.price());
        }
        if (req.maxCount() != null) {
            if (req.maxCount() < 0) throw new IllegalArgumentException("정원은 0 이상이어야 합니다.");
            c.setMaxCount(req.maxCount());
        }
        if (req.classTime() != null) c.setClassTime(req.classTime());
        if (req.active() != null) c.setActive(req.active());
        CourseEntity saved = courseJpaRepository.save(c);
        audit(adminUserId, "UPDATE_COURSE", "COURSE", id, ip, req.toString());
        return saved;
    }

    @Transactional
    public CourseSessionEntity createSession(Long courseId, AdminDtos.SessionCreateRequest req, Long adminUserId, String ip) {
        LocalTime start = LocalTime.parse(req.startTime());
        LocalTime end = LocalTime.parse(req.endTime());
        if (!start.isBefore(end)) throw new IllegalArgumentException("시작시간은 종료시간보다 빨라야 합니다.");

        boolean conflict = courseSessionJpaRepository.existsRoomTimeConflict(req.room(), req.dayOfWeek(), start, end, null);
        if (conflict) throw new IllegalArgumentException("같은 강의실/시간에 이미 편성된 강의가 있습니다.");

        CourseSessionEntity s = new CourseSessionEntity();
        s.setCourseId(courseId);
        s.setSection(req.section());
        s.setDayOfWeek(req.dayOfWeek());
        s.setStartTime(start);
        s.setEndTime(end);
        s.setRoom(req.room());
        s.setMaxCount(req.maxCount() == null ? 0 : req.maxCount());
        s.setEnrolledCount(0);
        s.setStatus("OPEN");
        CourseSessionEntity saved = courseSessionJpaRepository.save(s);
        audit(adminUserId, "CREATE_SESSION", "COURSE_SESSION", saved.getId(), ip, req.toString());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCourses() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CourseEntity c : courseJpaRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("courseCode", c.getCourseCode());
            row.put("title", c.getTitle());
            row.put("classTime", c.getClassTime() == null || c.getClassTime().isBlank() ? "-" : c.getClassTime());
            row.put("professor", c.getProfessor());
            row.put("price", c.getPrice());
            row.put("maxCount", c.getMaxCount() == null ? 0 : c.getMaxCount());
            out.add(row);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<CourseSessionEntity> getSessions(Long courseId) {
        return courseId == null ? courseSessionJpaRepository.findAll() : courseSessionJpaRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentEntity> getEnrollmentsByStatus(String status) {
        return enrollmentJpaRepository.findByStatusOrderByIdAsc(status);
    }

    @Transactional
    public EnrollmentEntity approveEnrollment(Long enrollmentId, Long adminUserId, String ip) {
        EnrollmentEntity e = enrollmentJpaRepository.findById(enrollmentId).orElseThrow();
        CourseSessionEntity s = courseSessionJpaRepository.findById(e.getCourseSessionId()).orElseThrow();

        if (s.getEnrolledCount() != null && s.getMaxCount() != null && s.getEnrolledCount() >= s.getMaxCount()) {
            e.setStatus("WAITLIST");
        } else {
            e.setStatus("ENROLLED");
            e.setEnrolledAt(java.time.LocalDateTime.now());
            s.setEnrolledCount((s.getEnrolledCount() == null ? 0 : s.getEnrolledCount()) + 1);
            courseSessionJpaRepository.save(s);
        }
        EnrollmentEntity saved = enrollmentJpaRepository.save(e);
        audit(adminUserId, "APPROVE_ENROLLMENT", "ENROLLMENT", enrollmentId, ip, saved.getStatus());
        return saved;
    }

    @Transactional
    public EnrollmentEntity rejectEnrollment(Long enrollmentId, Long adminUserId, String ip) {
        EnrollmentEntity e = enrollmentJpaRepository.findById(enrollmentId).orElseThrow();
        e.setStatus("REJECTED");
        EnrollmentEntity saved = enrollmentJpaRepository.save(e);
        audit(adminUserId, "REJECT_ENROLLMENT", "ENROLLMENT", enrollmentId, ip, "REJECTED");
        return saved;
    }

    @Transactional
    public AttendanceRecordEntity attendanceCheck(AdminDtos.AttendanceCheckRequest req, Long adminUserId, String ip) {
        AttendanceRecordEntity r = new AttendanceRecordEntity();
        r.setEnrollmentId(req.enrollmentId());
        r.setSessionDate(req.sessionDate());
        r.setStatus(req.status());
        r.setMinutesAttended(req.minutesAttended());
        r.setMinutesTotal(req.minutesTotal());
        r.setRecordedBy(String.valueOf(adminUserId));
        AttendanceRecordEntity saved = attendanceRecordJpaRepository.save(r);
        audit(adminUserId, "ATTENDANCE_CHECK", "ATTENDANCE", saved.getId(), ip, req.toString());
        return saved;
    }

    @Transactional
    public ProgressRecordEntity updateProgress(AdminDtos.ProgressUpdateRequest req, Long adminUserId, String ip) {
        ProgressRecordEntity p = new ProgressRecordEntity();
        p.setEnrollmentId(req.enrollmentId());
        p.setUnitId(req.unitId());
        p.setProgressPercent(req.progressPercent());
        p.setCompleted(Boolean.TRUE.equals(req.completed()));
        ProgressRecordEntity saved = progressRecordJpaRepository.save(p);
        audit(adminUserId, "UPDATE_PROGRESS", "PROGRESS", saved.getId(), ip, req.toString());
        return saved;
    }

    @Transactional
    public UserEntity updateUserRole(Long userId, String role, Long adminUserId, String ip) {
        UserEntity u = userJpaRepository.findById(userId).orElseThrow();
        u.setRole(role);
        UserEntity saved = userJpaRepository.save(u);
        audit(adminUserId, "UPDATE_USER_ROLE", "USER", userId, ip, role);
        return saved;
    }

    @Transactional
    public UserEntity updateUserEnabled(Long userId, Boolean enabled, Long adminUserId, String ip) {
        UserEntity u = userJpaRepository.findById(userId).orElseThrow();
        u.setEnabled(Boolean.TRUE.equals(enabled));
        UserEntity saved = userJpaRepository.save(u);
        audit(adminUserId, "UPDATE_USER_ENABLED", "USER", userId, ip, String.valueOf(enabled));
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courses", courseJpaRepository.count());
        m.put("sessions", courseSessionJpaRepository.count());
        m.put("users", userJpaRepository.count());
        m.put("enrolled", enrollmentJpaRepository.findByStatusOrderByIdAsc("ENROLLED").size());
        m.put("waitlist", enrollmentJpaRepository.findByStatusOrderByIdAsc("WAITLIST").size());
        m.put("applied", enrollmentJpaRepository.findByStatusOrderByIdAsc("REQUESTED").size());
        return m;
    }

    @Transactional
    public void deleteCourse(Long courseId, Long adminUserId, String ip) {
        enrollmentJpaRepository.deleteAttendanceByCourseId(courseId);
        enrollmentJpaRepository.deleteProgressByCourseId(courseId);
        enrollmentJpaRepository.deleteEnrollmentsByCourseId(courseId);
        courseSessionJpaRepository.deleteByCourseId(courseId);
        courseJpaRepository.deleteById(courseId);
        audit(adminUserId, "DELETE_COURSE", "COURSE", courseId, ip, "deleted");
    }

    private void audit(Long actorUserId, String action, String targetType, Long targetId, String ip, String afterJson) {
        AuditLogEntity a = new AuditLogEntity();
        a.setActorUserId(actorUserId);
        a.setAction(action);
        a.setTargetType(targetType);
        a.setTargetId(targetId);
        a.setIpAddress(ip);
        a.setAfterJson(afterJson);
        auditLogJpaRepository.save(a);
    }
}
