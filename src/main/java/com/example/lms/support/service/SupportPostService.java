package com.example.lms.support.service;

import com.example.lms.support.entity.PostStatus;
import com.example.lms.support.entity.SupportPostEntity;
import com.example.lms.support.repo.SupportPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportPostService {
    private final SupportPostRepository supportPostRepository;

    public SupportPostService(SupportPostRepository supportPostRepository) {
        this.supportPostRepository = supportPostRepository;
    }

    @Transactional
    public SupportPostEntity createPost(String title, String content, Long writerUserId, String writerLoginId, String writerDisplayName) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("제목을 입력해 주세요.");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("문의 내용을 입력해 주세요.");
        if (writerLoginId == null || writerLoginId.isBlank()) throw new IllegalArgumentException("작성자 정보가 올바르지 않습니다.");

        SupportPostEntity p = new SupportPostEntity();
        p.setTitle(title.trim());
        p.setContent(content.trim());
        p.setWriter(writerDisplayName == null || writerDisplayName.isBlank() ? writerLoginId : writerDisplayName.trim());
        p.setWriterLoginId(writerLoginId.trim());
        p.setWriterUserId(writerUserId);
        p.setStatus(PostStatus.WAITING);
        return supportPostRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<SupportPostEntity> getPostsByWriterLoginId(String writerLoginId) {
        return supportPostRepository.findByWriterLoginIdOrderByIdDesc(writerLoginId);
    }

    @Transactional(readOnly = true)
    public List<SupportPostEntity> getPosts() {
        return supportPostRepository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public SupportPostEntity getPost(Long id) {
        return supportPostRepository.findById(id).orElseThrow();
    }

    @Transactional
    public SupportPostEntity answerPost(Long id, String answer) {
        SupportPostEntity p = getPost(id);
        String normalized = answer == null ? "" : answer.trim();
        p.setAnswer(normalized.isBlank() ? null : normalized);
        p.setStatus(normalized.isBlank() ? PostStatus.WAITING : PostStatus.ANSWERED);
        return supportPostRepository.save(p);
    }

    @Transactional
    public SupportPostEntity updatePost(Long id, String title, String content) {
        SupportPostEntity p = getPost(id);
        if (title != null && !title.isBlank()) p.setTitle(title.trim());
        if (content != null && !content.isBlank()) p.setContent(content.trim());
        return supportPostRepository.save(p);
    }

    @Transactional
    public void deletePost(Long id) {
        supportPostRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countWaiting() {
        return supportPostRepository.countByStatus(PostStatus.WAITING);
    }
}
