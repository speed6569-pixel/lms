package com.example.lms.posts.service;

import com.example.lms.posts.dto.PostDtos;
import com.example.lms.posts.entity.PostEntity;
import com.example.lms.posts.repo.PostJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    private final PostJpaRepository postJpaRepository;

    public PostService(PostJpaRepository postJpaRepository) {
        this.postJpaRepository = postJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<PostEntity> adminList(String category, String q) {
        if (category != null && !category.isBlank() && q != null && !q.isBlank()) {
            return postJpaRepository.findByDeletedFalseAndCategoryAndTitleContainingIgnoreCaseOrderByPinnedDescIdDesc(category, q.trim());
        }
        if (category != null && !category.isBlank()) {
            return postJpaRepository.findByDeletedFalseAndCategoryOrderByPinnedDescIdDesc(category);
        }
        if (q != null && !q.isBlank()) {
            return postJpaRepository.findByDeletedFalseAndTitleContainingIgnoreCaseOrderByPinnedDescIdDesc(q.trim());
        }
        return postJpaRepository.findByDeletedFalseOrderByPinnedDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<PostEntity> userList(String category) {
        return postJpaRepository.findByDeletedFalseAndCategoryAndStatusOrderByPinnedDescIdDesc(category, "PUBLISHED");
    }

    @Transactional(readOnly = true)
    public PostEntity get(Long id) {
        return postJpaRepository.findById(id).orElseThrow();
    }

    @Transactional
    public PostEntity create(PostDtos.AdminPostSaveRequest req, Long adminId) {
        validate(req);
        PostEntity p = new PostEntity();
        p.setCategory(req.category());
        p.setTitle(req.title());
        p.setContent(req.content());
        p.setStatus(req.status() == null ? "DRAFT" : req.status());
        p.setPinned(Boolean.TRUE.equals(req.pinned()));
        p.setDeleted(false);
        p.setAuthorAdminId(adminId);
        return postJpaRepository.save(p);
    }

    @Transactional
    public PostEntity update(Long id, PostDtos.AdminPostSaveRequest req, Long adminId) {
        validate(req);
        PostEntity p = postJpaRepository.findById(id).orElseThrow();
        p.setCategory(req.category());
        p.setTitle(req.title());
        p.setContent(req.content());
        p.setStatus(req.status() == null ? p.getStatus() : req.status());
        p.setPinned(Boolean.TRUE.equals(req.pinned()));
        if (p.getAuthorAdminId() == null) p.setAuthorAdminId(adminId);
        return postJpaRepository.save(p);
    }

    @Transactional
    public void archive(Long id) {
        PostEntity p = postJpaRepository.findById(id).orElseThrow();
        p.setStatus("ARCHIVED");
        postJpaRepository.save(p);
    }

    @Transactional
    public void softDelete(Long id) {
        PostEntity p = postJpaRepository.findById(id).orElseThrow();
        p.setDeleted(true);
        postJpaRepository.save(p);
    }

    @Transactional
    public void publish(Long id) {
        PostEntity p = postJpaRepository.findById(id).orElseThrow();
        p.setStatus("PUBLISHED");
        postJpaRepository.save(p);
    }

    private void validate(PostDtos.AdminPostSaveRequest req) {
        if (req.category() == null || req.category().isBlank()) throw new IllegalArgumentException("카테고리를 선택해 주세요.");
        if (req.title() == null || req.title().isBlank()) throw new IllegalArgumentException("제목을 입력해 주세요.");
        if (req.content() == null || req.content().isBlank()) throw new IllegalArgumentException("내용을 입력해 주세요.");
    }
}
