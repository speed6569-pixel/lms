package com.example.lms.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[LMS] 이메일 인증번호 안내");
        message.setText("인증번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요.");

        mailSender.send(message);
    }

    public void sendPasswordResetLink(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[LMS] 비밀번호 재설정 안내");
        message.setText("아래 링크에서 비밀번호를 재설정해주세요.\n" + resetLink + "\n\n링크 유효시간: 60분");
        mailSender.send(message);
    }
}
