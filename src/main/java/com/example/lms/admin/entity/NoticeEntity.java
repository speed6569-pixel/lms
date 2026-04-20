package com.example.lms.admin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class NoticeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "author_id")
    private Long authorId;
    private Boolean published;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
