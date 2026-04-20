package com.example.lms.posts.dto;

public class PostDtos {
    public record AdminPostSaveRequest(String category, String title, String content, String status, Boolean pinned) {}
}
