package com.example.lms.auth.service;

import com.example.lms.auth.entity.PasswordResetTokenEntity;
import com.example.lms.auth.repo.PasswordResetTokenJpaRepository;
import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserJpaRepository userJpaRepository;
    private final PasswordResetTokenJpaRepository passwordResetTokenJpaRepository;
    private final EmailSenderService emailSenderService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserJpaRepository userJpaRepository,
                                PasswordResetTokenJpaRepository passwordResetTokenJpaRepository,
                                EmailSenderService emailSenderService,
                                PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordResetTokenJpaRepository = passwordResetTokenJpaRepository;
        this.emailSenderService = emailSenderService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestReset(String loginIdOrEmail, String baseUrl) {
        if (loginIdOrEmail == null || loginIdOrEmail.isBlank()) return;

        Optional<UserEntity> userOpt = findUser(loginIdOrEmail.trim());
        if (userOpt.isEmpty()) return;

        UserEntity user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) return;

        // 기존 미사용 토큰 무효화
        passwordResetTokenJpaRepository.findByUserIdAndUsedFalse(user.getId())
                .forEach(t -> t.setUsed(true));

        String token = generateSecureToken();
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setUserId(user.getId());
        entity.setToken(token);
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(60));
        entity.setUsed(false);
        passwordResetTokenJpaRepository.save(entity);

        String link = baseUrl + "/reset-password?token=" + token;
        try {
            emailSenderService.sendPasswordResetLink(user.getEmail(), link);
        } catch (Exception ex) {
            log.warn("[DEV] password reset mail send failed. link={}", link, ex);
            // 개발 fallback: 콘솔 로그
            System.out.println("[DEV RESET LINK] " + link);
        }
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) return false;
        return passwordResetTokenJpaRepository.findByToken(token)
                .filter(t -> !Boolean.TRUE.equals(t.getUsed()))
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenEntity prt = passwordResetTokenJpaRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (Boolean.TRUE.equals(prt.getUsed()) || prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 토큰입니다.");
        }

        UserEntity user = userJpaRepository.findById(prt.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        prt.setUsed(true);
    }

    private Optional<UserEntity> findUser(String loginIdOrEmail) {
        if (loginIdOrEmail.contains("@")) {
            Optional<UserEntity> byEmail = userJpaRepository.findByEmail(loginIdOrEmail);
            if (byEmail.isPresent()) return byEmail;
        }
        return userJpaRepository.findByLoginId(loginIdOrEmail);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
