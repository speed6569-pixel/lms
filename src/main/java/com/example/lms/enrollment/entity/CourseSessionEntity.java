package com.example.lms.enrollment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course_sessions")
public class CourseSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
}
