package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
}
