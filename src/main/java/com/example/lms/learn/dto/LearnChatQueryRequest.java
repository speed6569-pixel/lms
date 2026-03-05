package com.example.lms.learn.dto;

public record LearnChatQueryRequest(
        String question,
        String ragContext
) {
}
