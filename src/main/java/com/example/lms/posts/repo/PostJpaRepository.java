package com.example.lms.posts.repo;

import com.example.lms.posts.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostJpaRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findByDeletedFalseAndCategoryAndStatusOrderByPinnedDescIdDesc(String category, String status);
    List<PostEntity> findByDeletedFalseAndCategoryAndTitleContainingIgnoreCaseOrderByPinnedDescIdDesc(String category, String title);
    List<PostEntity> findByDeletedFalseAndCategoryOrderByPinnedDescIdDesc(String category);
    List<PostEntity> findByDeletedFalseAndTitleContainingIgnoreCaseOrderByPinnedDescIdDesc(String title);
    List<PostEntity> findByDeletedFalseOrderByPinnedDescIdDesc();
}
