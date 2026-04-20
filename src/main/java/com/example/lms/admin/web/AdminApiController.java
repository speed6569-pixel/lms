package com.example.lms.admin.web;

import com.example.lms.admin.dto.AdminDtos;
import com.example.lms.admin.service.AdminService;
import com.example.lms.admin.service.JobMetaService;
import com.example.lms.enrollment.repo.UserJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
public class AdminApiController {

    private final AdminService adminService;
    private final JobMetaService jobMetaService;
    private final UserJpaRepository userJpaRepository;

    public AdminApiController(AdminService adminService, JobMetaService jobMetaService, UserJpaRepository userJpaRepository) {
        this.adminService = adminService;
        this.jobMetaService = jobMetaService;
        this.userJpaRepository = userJpaRepository;
    }

    @GetMapping("/courses")
    public ResponseEntity<?> courses() {
        return ResponseEntity.ok(adminService.getCourses());
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(@RequestBody AdminDtos.CourseCreateRequest req,
                                          Authentication auth,
                                          HttpServletRequest request) {
        return ResponseEntity.ok(adminService.createCourse(req, adminId(auth), request.getRemoteAddr()));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable Long id,
                                          @RequestBody AdminDtos.CourseUpdateRequest req,
                                          Authentication auth,
                                          HttpServletRequest request) {
        return ResponseEntity.ok(adminService.updateCourse(id, req, adminId(auth), request.getRemoteAddr()));
    }

    @PatchMapping("/courses/{id}/status")
    public ResponseEntity<?> courseStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          Authentication auth,
                                          HttpServletRequest request) {
        String status = body.getOrDefault("status", "OPEN");
        return ResponseEntity.ok(adminService.updateCourseStatus(id, status, adminId(auth), request.getRemoteAddr()));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id,
                                          Authentication auth,
                                          HttpServletRequest request) {
        adminService.deleteCourse(id, adminId(auth), request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/courses/{id}/sessions")
    public ResponseEntity<?> createSession(@PathVariable Long id,
                                           @RequestBody AdminDtos.SessionCreateRequest req,
                                           Authentication auth,
                                           HttpServletRequest request) {
        return ResponseEntity.ok(adminService.createSession(id, req, adminId(auth), request.getRemoteAddr()));
    }

    @GetMapping("/courses/{id}/lessons")
    public ResponseEntity<?> getLessons(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getLessons(id));
    }

    @PostMapping("/courses/{id}/lessons")
    public ResponseEntity<?> addLessons(@PathVariable Long id, @RequestBody List<AdminDtos.LessonInput> req) {
        return ResponseEntity.ok(adminService.addLessons(id, req));
    }

    @PatchMapping("/lessons/{lessonId}")
    public ResponseEntity<?> updateLesson(@PathVariable Long lessonId, @RequestBody AdminDtos.LessonInput req) {
        return ResponseEntity.ok(adminService.updateLesson(lessonId, req));
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(adminService.deleteLesson(lessonId));
    }

    @PostMapping("/uploads/video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(adminService.uploadVideo(file));
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> sessions(@RequestParam(required = false) Long courseId) {
        return ResponseEntity.ok(adminService.getSessions(courseId));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<?> getEnrollments(@RequestParam(defaultValue = "REQUESTED") String status) {
        return ResponseEntity.ok(adminService.getEnrollmentsByStatus(status));
    }

    @PostMapping("/enrollments/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(adminService.approveEnrollment(id, adminId(auth), request.getRemoteAddr()));
    }

    @PostMapping("/enrollments/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(adminService.rejectEnrollment(id, adminId(auth), request.getRemoteAddr()));
    }

    @GetMapping("/courses/{id}/learners")
    public ResponseEntity<?> learners(@PathVariable Long id,
                                      @RequestParam(required = false) String q) {
        return ResponseEntity.ok(adminService.getCourseLearners(id, q));
    }

    @PostMapping("/attendance/check")
    public ResponseEntity<?> attendance(@RequestBody AdminDtos.AttendanceCheckRequest req,
                                        Authentication auth,
                                        HttpServletRequest request) {
        return ResponseEntity.ok(adminService.attendanceCheck(req, adminId(auth), request.getRemoteAddr()));
    }

    @PostMapping("/progress/update")
    public ResponseEntity<?> progress(@RequestBody AdminDtos.ProgressUpdateRequest req,
                                      Authentication auth,
                                      HttpServletRequest request) {
        return ResponseEntity.ok(adminService.updateProgress(req, adminId(auth), request.getRemoteAddr()));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> userRole(@PathVariable Long id,
                                      @RequestBody AdminDtos.UserRoleRequest req,
                                      Authentication auth,
                                      HttpServletRequest request) {
        return ResponseEntity.ok(adminService.updateUserRole(id, req.role(), adminId(auth), request.getRemoteAddr()));
    }

    @PatchMapping("/users/{id}/enabled")
    public ResponseEntity<?> userEnabled(@PathVariable Long id,
                                         @RequestBody AdminDtos.UserEnabledRequest req,
                                         Authentication auth,
                                         HttpServletRequest request) {
        return ResponseEntity.ok(adminService.updateUserEnabled(id, req.enabled(), adminId(auth), request.getRemoteAddr()));
    }

    @GetMapping("/job-meta")
    public Map<String, Object> jobMeta() {
        return Map.of(
                "groups", jobMetaService.getActiveGroups(),
                "levelsMap", jobMetaService.getActiveLevelsMap()
        );
    }

    @PostMapping("/job-groups")
    public ResponseEntity<?> addJobGroup(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(jobMetaService.addGroup(body.get("name")));
    }

    @PostMapping("/job-levels")
    public ResponseEntity<?> addJobLevel(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(jobMetaService.addLevel(body.get("groupName"), body.get("name")));
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.stats();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
    }

    private Long adminId(Authentication auth) {
        return userJpaRepository.findByLoginId(auth.getName()).map(u -> u.getId()).orElse(0L);
    }
}
