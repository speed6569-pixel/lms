package com.example.lms.auth.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EmailVerificationService {

    private static final String KEY_EMAIL = "signup.verification.email";
    private static final String KEY_CODE = "signup.verification.code";
    private static final String KEY_EXPIRES_AT = "signup.verification.expiresAt";
    private static final String KEY_LAST_SENT_AT = "signup.verification.lastSentAt";

    private static final long EXPIRE_SECONDS = 300; // 5분
    private static final long RESEND_COOLTIME_SECONDS = 60; // 60초

    public String issueCode(String email, HttpSession session) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

        session.setAttribute(KEY_EMAIL, email);
        session.setAttribute(KEY_CODE, code);
        session.setAttribute(KEY_EXPIRES_AT, Instant.now().plusSeconds(EXPIRE_SECONDS).toEpochMilli());
        session.setAttribute(KEY_LAST_SENT_AT, Instant.now().toEpochMilli());

        return code;
    }

    public boolean canResend(HttpSession session) {
        return getRemainResendSeconds(session) == 0;
    }

    public long getRemainResendSeconds(HttpSession session) {
        Object lastSentAt = session.getAttribute(KEY_LAST_SENT_AT);
        if (lastSentAt == null) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        long elapsedSeconds = (now - (long) lastSentAt) / 1000;
        long remain = RESEND_COOLTIME_SECONDS - elapsedSeconds;

        return Math.max(remain, 0);
    }

    public long getRemainExpireSeconds(HttpSession session) {
        Object expiresAt = session.getAttribute(KEY_EXPIRES_AT);
        if (expiresAt == null) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        long remainMillis = (long) expiresAt - now;
        if (remainMillis <= 0) {
            return 0;
        }

        return (remainMillis + 999) / 1000;
    }

    public boolean isValid(String email, String inputCode, HttpSession session) {
        Object savedEmail = session.getAttribute(KEY_EMAIL);
        Object savedCode = session.getAttribute(KEY_CODE);
        Object expiresAt = session.getAttribute(KEY_EXPIRES_AT);

        if (savedEmail == null || savedCode == null || expiresAt == null) {
            return false;
        }

        long expireTime = (long) expiresAt;
        boolean notExpired = Instant.now().toEpochMilli() <= expireTime;

        return notExpired
                && email.equals(savedEmail)
                && inputCode != null
                && inputCode.equals(savedCode);
    }
}
