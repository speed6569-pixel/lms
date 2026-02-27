package com.example.lms.enrollment.repo;

import com.example.lms.enrollment.entity.LoginHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryJpaRepository extends JpaRepository<LoginHistoryEntity, Long> {
    List<LoginHistoryEntity> findTop20ByUserIdOrderByLoginTimeDesc(Long userId);
    void deleteByUserId(Long userId);
}
