package com.example.lms.support.repo;

import com.example.lms.support.entity.PostStatus;
import com.example.lms.support.entity.SupportPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportPostRepository extends JpaRepository<SupportPostEntity, Long> {
    List<SupportPostEntity> findByWriterOrderByIdDesc(String writer);
    List<SupportPostEntity> findByStatusOrderByIdDesc(PostStatus status);
    List<SupportPostEntity> findAllByOrderByIdDesc();
    List<SupportPostEntity> findTop5ByOrderByCreatedAtDesc();
    long countByStatus(PostStatus status);
}
