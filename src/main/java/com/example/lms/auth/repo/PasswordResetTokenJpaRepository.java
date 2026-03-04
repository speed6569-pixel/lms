package com.example.lms.auth.repo;

import com.example.lms.auth.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
    List<PasswordResetTokenEntity> findByUserIdAndUsedFalse(Long userId);
}
