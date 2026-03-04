package com.example.lms.settings.service;

import com.example.lms.enrollment.entity.LoginHistoryEntity;
import com.example.lms.enrollment.entity.PointTransactionEntity;
import com.example.lms.enrollment.entity.PointTransactionType;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.EnrollmentJpaRepository;
import com.example.lms.enrollment.repo.LoginHistoryJpaRepository;
import com.example.lms.enrollment.repo.PointTransactionJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import com.example.lms.settings.dto.SettingsDtos;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SettingsService {

    private final UserJpaRepository userJpaRepository;
    private final LoginHistoryJpaRepository loginHistoryJpaRepository;
    private final EnrollmentJpaRepository enrollmentJpaRepository;
    private final PointTransactionJpaRepository pointTransactionJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(UserJpaRepository userJpaRepository,
                           LoginHistoryJpaRepository loginHistoryJpaRepository,
                           EnrollmentJpaRepository enrollmentJpaRepository,
                           PointTransactionJpaRepository pointTransactionJpaRepository,
                           PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.loginHistoryJpaRepository = loginHistoryJpaRepository;
        this.enrollmentJpaRepository = enrollmentJpaRepository;
        this.pointTransactionJpaRepository = pointTransactionJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public SettingsDtos.MeResponse getMe(String loginId) {
        UserEntity user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<LoginHistoryEntity> rows = loginHistoryJpaRepository.findTop20ByUserIdOrderByLoginTimeDesc(user.getId());
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<SettingsDtos.LoginHistoryItem> history = rows.stream()
                .map(v -> new SettingsDtos.LoginHistoryItem(
                        v.getLoginTime() == null ? "" : v.getLoginTime().format(f),
                        valueOrDash(v.getIpAddress()),
                        valueOrDash(v.getUserAgent())
                ))
                .toList();

        Map<String, SettingsDtos.DeviceItem> deviceMap = new LinkedHashMap<>();
        for (LoginHistoryEntity v : rows) {
            String key = valueOrDash(v.getIpAddress()) + "|" + valueOrDash(v.getUserAgent());
            if (!deviceMap.containsKey(key)) {
                deviceMap.put(key, new SettingsDtos.DeviceItem(
                        valueOrDash(v.getIpAddress()),
                        valueOrDash(v.getUserAgent()),
                        v.getLoginTime() == null ? "" : v.getLoginTime().format(f)
                ));
            }
        }

        return new SettingsDtos.MeResponse(
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getPointBalance() == null ? 0 : user.getPointBalance(),
                history,
                deviceMap.values().stream().toList()
        );
    }

    @Transactional
    public SettingsDtos.MeResponse updateProfile(String loginId, SettingsDtos.UpdateProfileRequest request) {
        UserEntity user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("이름을 입력해 주세요.");
        }
        user.setName(request.name().trim());
        user.setPhone(request.phone() == null ? null : request.phone().trim());
        userJpaRepository.save(user);

        return getMe(loginId);
    }

    @Transactional
    public void changePassword(String loginId, SettingsDtos.ChangePasswordRequest request) {
        UserEntity user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (request.currentPassword() == null || request.newPassword() == null
                || request.currentPassword().isBlank() || request.newPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호와 새 비밀번호를 입력해 주세요.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userJpaRepository.save(user);
    }

    @Transactional
    public void withdraw(String loginId) {
        UserEntity user = userJpaRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        enrollmentJpaRepository.deleteAttendanceByUserId(user.getId());
        enrollmentJpaRepository.deleteProgressByUserId(user.getId());
        enrollmentJpaRepository.deleteEnrollmentsByUserId(user.getId());
        loginHistoryJpaRepository.deleteByUserId(user.getId());
        userJpaRepository.delete(user);
    }

    @Transactional
    public int chargePoints(String loginId, int chargePlan) {
        UserEntity user = userJpaRepository.findByLoginId(loginId)
                .flatMap(u -> userJpaRepository.findByIdForUpdate(u.getId()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        int earn = switch (chargePlan) {
            case 5000 -> 5000;
            case 10000 -> 12000;
            case 50000 -> 100000;
            default -> throw new IllegalArgumentException("지원하지 않는 충전 금액입니다.");
        };

        int nextBalance = (user.getPointBalance() == null ? 0 : user.getPointBalance()) + earn;
        user.setPointBalance(nextBalance);

        PointTransactionEntity tx = new PointTransactionEntity();
        tx.setUserId(user.getId());
        tx.setType(PointTransactionType.EARN);
        tx.setAmount(earn);
        tx.setBalanceAfter(nextBalance);
        tx.setMemo("충전 " + chargePlan + "원");
        pointTransactionJpaRepository.save(tx);

        return earn;
    }

    public void saveLoginHistory(String loginId, String ipAddress, String userAgent) {
        userJpaRepository.findByLoginId(loginId).ifPresent(user -> {
            LoginHistoryEntity entity = new LoginHistoryEntity();
            entity.setUserId(user.getId());
            entity.setLoginTime(java.time.LocalDateTime.now());
            entity.setIpAddress(ipAddress);
            entity.setUserAgent(userAgent);
            loginHistoryJpaRepository.save(entity);
        });
    }

    private String valueOrDash(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
