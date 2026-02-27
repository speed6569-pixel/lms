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
    private final AdminCourseSessionJpaRepository adminCourseSessionJpaRepository;
    private final CourseSessionJpaRepository courseSessionJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final AttendanceRecordJpaRepository attendanceRecordJpaRepository;
    private final ProgressRecordJpaRepository progressRecordJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AuditLogJpaRepository auditLogJpaRepository;

    public AdminService(CourseJpaRepository courseJpaRepository,
                        AdminCourseSessionJpaRepository adminCourseSessionJpaRepository,
                        CourseSessionJpaRepository courseSessionJpaRepository,
                        EnrollmentJpaRepository enrollmentJpaRepository,
                        AttendanceRecordJpaRepository attendanceRecordJpaRepository,
                        ProgressRecordJpaRepository progressRecordJpaRepository,
                        UserJpaRepository userJpaRepository,
                        AuditLogJpaRepository auditLogJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.adminCourseSessionJpaRepository = adminCourseSessionJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.attendanceRecordJpaRepository = attendanceRecordJpaRepository;
        this.progressRecordJpaRepository = progressRecordJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.auditLogJpaRepository = auditLogJpaRepository;
    }

    @Transactional
    public CourseEntity createCourse(AdminDtos.CourseCreateRequest req, Long adminUserId, String ip) {
        if (req.subjectCode() == null || req.subjectCode().isBlank()) throw new IllegalArgumentException("과목코드는 필수입니다.");
        if (courseJpaRepository.existsBySubjectCode(req.subjectCode().trim()) || courseJpaRepository.existsByCourseCode(req.subjectCode().trim())) {
            throw new IllegalArgumentException("이미 존재하는 과목코드입니다.");
        }
        if (req.price() != null && req.price() < 0) throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        if (req.capacity() != null && req.capacity() < 0) throw new IllegalArgumentException("정원은 0 이상이어야 합니다.");

        CourseEntity c = new CourseEntity();
        c.setSubjectCode(req.subjectCode().trim());
        c.setCourseCode(req.subjectCode().trim());
        c.setJobGroup(req.jobGroup());
        c.setJobLevel(req.jobLevel());
        c.setSubjectName(req.subjectName());
        c.setTitle(req.subjectName());
        c.setInstructor(req.instructor());
        c.setProfessor(req.instructor());
        c.setDescription(req.description());
        c.setPrice(req.price() == null ? 0 : req.price());
        c.setCapacity(req.capacity() == null ? 0 : req.capacity());
        c.setMaxCount(req.capacity() == null ? 0 : req.capacity());
        c.setStatus(req.status() == null || req.status().isBlank() ? "OPEN" : req.status());
        c.setClassTime(null);
        c.setActive(true);
        CourseEntity saved = courseJpaRepository.save(c);

        List<AdminDtos.CourseSessionInput> sessions = req.sessions() == null ? List.of() : req.sessions();
        if (sessions.isEmpty()) {
            if (req.dayRange() == null || req.dayRange().isBlank() || req.startTime() == null || req.endTime() == null) {
                throw new IllegalArgumentException("요일 범위와 시작/종료 시간을 입력해 주세요.");
            }
            List<String> days = expandDayRange(req.dayRange());
            sessions = days.stream()
                    .map(d -> new AdminDtos.CourseSessionInput(d, req.startTime(), req.endTime(), null))
                    .toList();
        }

        int sectionNo = 1;
        for (AdminDtos.CourseSessionInput input : sessions) {
            LocalTime start = LocalTime.parse(input.startTime());
            LocalTime end = LocalTime.parse(input.endTime());
            if (!start.isBefore(end)) throw new IllegalArgumentException("세션 시작시간은 종료시간보다 빨라야 합니다.");

            AdminCourseSessionEntity s = new AdminCourseSessionEntity();
            s.setCourse(saved);
            s.setSection(String.format("%02d", sectionNo++));
            s.setDayOfWeek(input.dayOfWeek());
            s.setStartTime(start);
            s.setEndTime(end);
            s.setRoom(input.room());
            s.setMaxCount(saved.getCapacity() == null ? 0 : saved.getCapacity());
            s.setEnrolledCount(0);
            s.setStatus("OPEN");
            adminCourseSessionJpaRepository.save(s);
        }

        audit(adminUserId, "CREATE_COURSE", "COURSE", saved.getId(), ip, req.toString());
        return saved;
    }

    @Transactional
    public CourseEntity updateCourse(Long id, AdminDtos.CourseUpdateRequest req, Long adminUserId, String ip) {
        CourseEntity c = courseJpaRepository.findById(id).orElseThrow();
        if (req.subjectName() != null) { c.setSubjectName(req.subjectName()); c.setTitle(req.subjectName()); }
        if (req.instructor() != null) { c.setInstructor(req.instructor()); c.setProfessor(req.instructor()); }
        if (req.jobGroup() != null) c.setJobGroup(req.jobGroup());
        if (req.jobLevel() != null) c.setJobLevel(req.jobLevel());
        if (req.price() != null) {
            if (req.price() < 0) throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
            c.setPrice(req.price());
        }
        if (req.capacity() != null) {
            if (req.capacity() < 0) throw new IllegalArgumentException("정원은 0 이상이어야 합니다.");
            c.setCapacity(req.capacity());
            c.setMaxCount(req.capacity());
        }
        if (req.status() != null) c.setStatus(req.status());
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
            row.put("applyStatus", "CLOSED".equalsIgnoreCase(c.getStatus()) ? "마감" : "모집중");
            row.put("subjectCode", c.getSubjectCode() == null ? c.getCourseCode() : c.getSubjectCode());
            row.put("jobGroup", c.getJobGroup() == null ? "" : c.getJobGroup());
            row.put("jobLevel", c.getJobLevel() == null ? "" : c.getJobLevel());
            row.put("subjectName", c.getSubjectName() == null ? c.getTitle() : c.getSubjectName());
            row.put("instructor", c.getInstructor() == null ? c.getProfessor() : c.getInstructor());
            row.put("classTime", toDayDurationText(adminCourseSessionJpaRepository.findByCourse_Id(c.getId())));
            row.put("price", c.getPrice());
            row.put("capacity", c.getCapacity() == null ? (c.getMaxCount() == null ? 0 : c.getMaxCount()) : c.getCapacity());
            row.put("status", c.getStatus() == null ? "OPEN" : c.getStatus());
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
    public CourseEntity updateCourseStatus(Long courseId, String status, Long adminUserId, String ip) {
        CourseEntity c = courseJpaRepository.findById(courseId).orElseThrow();
        c.setStatus(status);
        CourseEntity saved = courseJpaRepository.save(c);
        audit(adminUserId, "UPDATE_COURSE_STATUS", "COURSE", courseId, ip, status);
        return saved;
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

    private List<String> expandDayRange(String range) {
        List<String> week = List.of("월", "화", "수", "목", "금", "토", "일");
        String[] parts = range.replace(" ", "").split("~");
        if (parts.length == 1) return List.of(parts[0]);
        int s = week.indexOf(parts[0]);
        int e = week.indexOf(parts[1]);
        if (s < 0 || e < 0 || s > e) throw new IllegalArgumentException("요일 범위 형식이 올바르지 않습니다. 예: 월~금");
        return week.subList(s, e + 1);
    }

    private String toDayDurationText(List<AdminCourseSessionEntity> sessions) {
        if (sessions == null || sessions.isEmpty()) return "-";
        Map<String, Integer> dayMinutes = new LinkedHashMap<>();
        for (AdminCourseSessionEntity s : sessions) {
            if (s.getStartTime() == null || s.getEndTime() == null) continue;
            int minutes = (s.getEndTime().getHour() * 60 + s.getEndTime().getMinute())
                    - (s.getStartTime().getHour() * 60 + s.getStartTime().getMinute());
            dayMinutes.merge(s.getDayOfWeek(), Math.max(minutes, 0), Integer::sum);
        }
        if (dayMinutes.isEmpty()) return "-";
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : dayMinutes.entrySet()) {
            double hours = e.getValue() / 60.0;
            String h = (hours == (int) hours) ? String.valueOf((int) hours) : String.format("%.1f", hours);
            parts.add(e.getKey() + " " + h + "시간");
        }
        return String.join(" / ", parts);
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
