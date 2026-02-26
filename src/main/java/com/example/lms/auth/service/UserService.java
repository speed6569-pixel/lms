package com.example.lms.auth.service;

import com.example.lms.enrollment.entity.UserEntity;
import com.example.lms.enrollment.repo.UserJpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserJpaRepository userJpaRepository, PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(String loginId, String rawPassword, String name) {
        if (userJpaRepository.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        UserEntity user = new UserEntity();
        user.setLoginId(loginId);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setName(name);
        user.setRole("ROLE_USER");
        userJpaRepository.save(user);
    }
}
