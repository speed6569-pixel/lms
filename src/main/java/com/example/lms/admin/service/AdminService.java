package com.example.lms.admin.service;

import com.example.lms.admin.dto.AdminDtos;
import com.example.lms.admin.dto.AdminEnrollmentRowDto;
import com.example.lms.admin.entity.*;
import com.example.lms.admin.repo.*;
import com.example.lms.enrollment.entity.CourseSessionEntity;
import com.example.lms.enrollment.entity.EnrollmentEntity;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.CourseSessionJpaRepository;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.enrollment.service.PointService;
import com.example.lms.learn.entity.LessonEntity;
import com.example.lms.learn.repo.LessonJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private final PointService pointService;
    private final LessonJpaRepository lessonJpaRepository;

    public AdminService(CourseJpaRepository courseJpaRepository,
                        AdminCourseSessionJpaRepository adminCourseSessionJpaRepository,
                        CourseSessionJpaRepository courseSessionJpaRepository,
                        EnrollmentJpaRepository enrollmentJpaRepository,
                        AttendanceRecordJpaRepository attendanceRecordJpaRepository,
                        ProgressRecordJpaRepository progressRecordJpaRepository,
                        UserJpaRepository userJpaRepository,
                        AuditLogJpaRepository auditLogJpaRepository,
                        PointService pointService,
                        LessonJpaRepository lessonJpaRepository) {
        this.courseJpaRepository = courseJpaRepository;
        this.adminCourseSessionJpaRepository = adminCourseSessionJpaRepository;
        this.courseSessionJpaRepository = courseSessionJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.attendanceRecordJpaRepository = attendanceRecordJpaRepository;
        this.progressRecordJpaRepository = progressRecordJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.pointService = pointService;
        this.lessonJpaRepository = lessonJpaRepository;
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
        c.setIsDeleted(false);
        CourseEntity saved = courseJpaRepository.save(c);

        List<AdminDtos.CourseSessionInput> sessions = req.sessions() == null ? List.of() : req.sessions();
        if (sessions.isEmpty()) {
            if (req.startTime() == null || req.endTime() == null) {
                throw new IllegalArgumentException("시작/종료 시간을 입력해 주세요.");
            }
            List<String> selectedDays = resolveSelectedDays(req.dayMode(), req.days(), req.startDay(), req.endDay());
            sessions = selectedDays.stream()
                    .map(d -> new AdminDtos.CourseSessionInput(d, req.startTime(), req.endTime(), null))
                    .toList();
        }

        int sectionNo = 1;
        for (AdminDtos.CourseSessionInput input : sessions) {
            LocalTime start = LocalTime.parse(input.startTime());
            LocalTime end = LocalTime.parse(input.endTime());
            if (!start.isBefore(end)) throw new IllegalArgumentException("세션 시작시간은 종료시간보다 빨라야 합니다.");

            String normalizedDay = normalizeDay(input.dayOfWeek());
            if (adminCourseSessionJpaRepository.existsByCourse_IdAndDayOfWeekAndStartTimeAndEndTime(saved.getId(), normalizedDay, start, end)) {
                throw new IllegalArgumentException("중복 세션이 이미 존재합니다: " + normalizedDay + " " + start + "~" + end);
            }

            AdminCourseSessionEntity s = new AdminCourseSessionEntity();
            s.setCourse(saved);
            s.setSection(String.format("%02d", sectionNo++));
            s.setDayOfWeek(normalizedDay);
            s.setStartTime(start);
            s.setEndTime(end);
            s.setRoom(input.room());
            s.setMaxCount(saved.getCapacity() == null ? 0 : saved.getCapacity());
            s.setEnrolledCount(0);
            s.setStatus("OPEN");
            adminCourseSessionJpaRepository.save(s);
        }

        List<AdminDtos.LessonInput> lessons = req.lessons() == null ? List.of() : req.lessons();
        for (AdminDtos.LessonInput li : lessons) {
            if (li.videoUrl() == null || li.videoUrl().isBlank()) continue;
            LessonEntity lesson = new LessonEntity();
            lesson.setCourseId(saved.getId());
            lesson.setTitle(li.title() == null || li.title().isBlank() ? "차시" : li.title().trim());
            lesson.setDescription(li.description());
            lesson.setVideoUrl(li.videoUrl().trim());
            lesson.setOrderNo(li.orderNo() == null ? 1 : li.orderNo());
            lesson.setThumbnailUrl(li.thumbnailUrl());
            lessonJpaRepository.save(lesson);
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
        for (CourseEntity c : courseJpaRepository.findByIsDeletedFalseOrderByIdDesc()) {
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
    public List<AdminEnrollmentRowDto> getEnrollmentsByStatus(String status) {
        Map<Long, java.util.List<AdminEnrollmentFlatProjection>> grouped = new LinkedHashMap<>();
        for (AdminEnrollmentFlatProjection r : enrollmentJpaRepository.findAdminEnrollmentRowsByStatus(status)) {
            grouped.computeIfAbsent(r.getEnrollmentId(), k -> new ArrayList<>()).add(r);
        }

        List<AdminEnrollmentRowDto> out = new ArrayList<>();
        for (var e : grouped.entrySet()) {
            var rows = e.getValue();
            var first = rows.get(0);
            String scheduleText = buildScheduleTextFromFlat(rows);
            out.add(new AdminEnrollmentRowDto(
                    first.getEnrollmentId(),
                    first.getUsername(),
                    first.getName(),
                    first.getSubjectCode(),
                    first.getCourseName(),
                    scheduleText,
                    first.getPrice() == null ? 0 : first.getPrice(),
                    first.getPaymentStatus(),
                    first.getStatus(),
                    first.getAppliedAt()
            ));
        }
        return out;
    }

    @Transactional
    public EnrollmentEntity approveEnrollment(Long enrollmentId, Long adminUserId, String ip) {
        EnrollmentEntity e = enrollmentJpaRepository.findById(enrollmentId).orElseThrow();
        CourseSessionEntity s = courseSessionJpaRepository.findById(e.getCourseSessionId()).orElseThrow();

        if ("CANCEL_REQUESTED".equalsIgnoreCase(e.getStatus())) {
            e.setStatus("CANCELLED");
            int current = s.getEnrolledCount() == null ? 0 : s.getEnrolledCount();
            s.setEnrolledCount(Math.max(0, current - 1));
            courseSessionJpaRepository.save(s);

            int refunded = pointService.refundCoursePayment(e.getUserId(), e.getCourseId(), "관리자 승인 취소 환불");
            EnrollmentEntity saved = enrollmentJpaRepository.save(e);
            audit(adminUserId, "APPROVE_CANCEL_REQUEST", "ENROLLMENT", enrollmentId, ip,
                    "status=CANCELLED,refunded=" + refunded);
            return saved;
        }

        if (s.getEnrolledCount() != null && s.getMaxCount() != null && s.getEnrolledCount() >= s.getMaxCount()) {
            e.setStatus("WAITLIST");
        } else {
            e.setStatus("APPROVED");
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

        if ("CANCEL_REQUESTED".equalsIgnoreCase(e.getStatus())) {
            String rollback = (e.getEnrolledAt() == null) ? "APPLIED" : "APPROVED";
            e.setStatus(rollback);
            EnrollmentEntity saved = enrollmentJpaRepository.save(e);
            audit(adminUserId, "REJECT_CANCEL_REQUEST", "ENROLLMENT", enrollmentId, ip, rollback);
            return saved;
        }

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
        m.put("approved", enrollmentJpaRepository.findByStatusOrderByIdAsc("APPROVED").size());
        m.put("waitlist", enrollmentJpaRepository.findByStatusOrderByIdAsc("WAITLIST").size());
        m.put("applied", enrollmentJpaRepository.findByStatusOrderByIdAsc("APPLIED").size());
        m.put("pendingRequests", enrollmentJpaRepository.findByStatusOrderByIdAsc("APPLIED").size() + enrollmentJpaRepository.findByStatusOrderByIdAsc("WAITLIST").size());
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
        long learnerCount = enrollmentJpaRepository.countByCourseIdAndStatusIn(courseId, List.of("APPROVED", "RUNNING"));
        CourseEntity c = courseJpaRepository.findById(courseId).orElseThrow();

        if (learnerCount > 0) {
            c.setIsDeleted(true);
            c.setActive(false);
            courseJpaRepository.save(c);
            audit(adminUserId, "SOFT_DELETE_COURSE", "COURSE", courseId, ip, "is_deleted=true");
            return;
        }

        enrollmentJpaRepository.deleteAttendanceByCourseId(courseId);
        enrollmentJpaRepository.deleteProgressByCourseId(courseId);
        enrollmentJpaRepository.deleteEnrollmentsByCourseId(courseId);
        lessonJpaRepository.deleteByCourseId(courseId);
        courseSessionJpaRepository.deleteByCourseId(courseId);
        courseJpaRepository.deleteById(courseId);
        audit(adminUserId, "DELETE_COURSE", "COURSE", courseId, ip, "hard-deleted");
    }

    private List<String> resolveSelectedDays(String dayMode, List<String> days, String startDay, String endDay) {
        if ("MULTI".equalsIgnoreCase(dayMode)) {
            if (days == null || days.isEmpty()) throw new IllegalArgumentException("개별 선택 요일을 1개 이상 선택해 주세요.");
            return days.stream().map(this::normalizeDay).distinct().toList();
        }
        if ("RANGE".equalsIgnoreCase(dayMode)) {
            if (startDay == null || endDay == null) throw new IllegalArgumentException("범위 시작/종료 요일을 선택해 주세요.");
            return expandDayRange(startDay, endDay);
        }
        throw new IllegalArgumentException("요일 선택 모드가 올바르지 않습니다.");
    }

    private List<String> expandDayRange(String startDay, String endDay) {
        List<String> week = List.of("월", "화", "수", "목", "금", "토", "일");
        String sNorm = normalizeDay(startDay);
        String eNorm = normalizeDay(endDay);
        int s = week.indexOf(sNorm);
        int e = week.indexOf(eNorm);
        if (s < 0 || e < 0 || s > e) throw new IllegalArgumentException("요일 범위 형식이 올바르지 않습니다. 예: MON~FRI 또는 월~금");
        return week.subList(s, e + 1);
    }

    private String normalizeDay(String day) {
        return switch (day) {
            case "MON", "월" -> "월";
            case "TUE", "화" -> "화";
            case "WED", "수" -> "수";
            case "THU", "목" -> "목";
            case "FRI", "금" -> "금";
            case "SAT", "토" -> "토";
            case "SUN", "일" -> "일";
            default -> throw new IllegalArgumentException("지원하지 않는 요일: " + day);
        };
    }

    private String buildScheduleTextFromFlat(List<AdminEnrollmentFlatProjection> sessions) {
        if (sessions == null || sessions.isEmpty()) return "-";
        List<String> order = List.of("월", "화", "수", "목", "금", "토", "일");
        Map<String, List<String>> timeToDays = new LinkedHashMap<>();
        for (AdminEnrollmentFlatProjection s : sessions) {
            String time = s.getStartTime() + "~" + s.getEndTime();
            timeToDays.computeIfAbsent(time, k -> new ArrayList<>()).add(s.getDay());
        }
        List<String> parts = new ArrayList<>();
        for (var en : timeToDays.entrySet()) {
            List<String> days = en.getValue().stream().distinct()
                    .sorted(Comparator.comparingInt(order::indexOf)).toList();
            parts.add(compressDays(days, order) + " " + en.getKey());
        }
        return String.join("; ", parts);
    }

    private String compressDays(List<String> days, List<String> order) {
        if (days.isEmpty()) return "";
        if (days.size() == 1) return days.get(0);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= days.size(); i++) {
            boolean broken = (i == days.size()) || (order.indexOf(days.get(i)) - order.indexOf(days.get(i - 1)) != 1);
            if (broken) {
                String from = days.get(start);
                String to = days.get(i - 1);
                if (start == i - 1) chunks.add(from);
                else if (i - start == 2) chunks.add(from + "/" + to);
                else chunks.add(from + "~" + to);
                start = i;
            }
        }
        return String.join("/", chunks);
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

    @Transactional(readOnly = true)
    public List<LessonEntity> getLessons(Long courseId) {
        return lessonJpaRepository.findByCourseIdOrderByOrderNoAsc(courseId);
    }

    @Transactional
    public Map<String, Object> addLessons(Long courseId, List<AdminDtos.LessonInput> lessons) {
        if (!courseJpaRepository.existsById(courseId)) throw new IllegalArgumentException("강의를 찾을 수 없습니다.");
        int added = 0;
        for (AdminDtos.LessonInput li : (lessons == null ? List.<AdminDtos.LessonInput>of() : lessons)) {
            if (li.videoUrl() == null || li.videoUrl().isBlank()) continue;
            LessonEntity l = new LessonEntity();
            l.setCourseId(courseId);
            l.setTitle(li.title() == null || li.title().isBlank() ? "차시" : li.title());
            l.setDescription(li.description());
            l.setVideoUrl(li.videoUrl());
            l.setOrderNo(li.orderNo() == null ? 1 : li.orderNo());
            l.setThumbnailUrl(li.thumbnailUrl());
            lessonJpaRepository.save(l);
            added++;
        }
        return Map.of("success", true, "added", added);
    }

    @Transactional
    public LessonEntity updateLesson(Long lessonId, AdminDtos.LessonInput req) {
        LessonEntity l = lessonJpaRepository.findById(lessonId).orElseThrow();
        if (req.title() != null) l.setTitle(req.title());
        if (req.description() != null) l.setDescription(req.description());
        if (req.videoUrl() != null && !req.videoUrl().isBlank()) l.setVideoUrl(req.videoUrl());
        if (req.orderNo() != null) l.setOrderNo(req.orderNo());
        if (req.thumbnailUrl() != null) l.setThumbnailUrl(req.thumbnailUrl());
        return lessonJpaRepository.save(l);
    }

    @Transactional
    public Map<String, Object> deleteLesson(Long lessonId) {
        lessonJpaRepository.deleteById(lessonId);
        return Map.of("success", true);
    }

    @Transactional
    public Map<String, Object> uploadVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        String original = file.getOriginalFilename() == null ? "video.mp4" : file.getOriginalFilename();
        String lower = original.toLowerCase();
        if (!(lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov"))) {
            throw new IllegalArgumentException("mp4/webm/mov 파일만 업로드 가능합니다.");
        }

        try {
            Path dir = Path.of("/home/ubuntu/project/lms/uploads/videos");
            Files.createDirectories(dir);
            String ext = lower.substring(lower.lastIndexOf('.'));
            String savedName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path path = dir.resolve(savedName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return Map.of("success", true, "url", "/media/videos/" + savedName);
        } catch (IOException e) {
            throw new IllegalArgumentException("영상 업로드에 실패했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<AdminCourseLearnerProjection> getCourseLearners(Long courseId, String q) {
        return enrollmentJpaRepository.findCourseLearners(courseId, q);
    }

    @Transactional(readOnly = true)
    public CourseEntity getCourse(Long courseId) {
        return courseJpaRepository.findById(courseId).orElseThrow();
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
