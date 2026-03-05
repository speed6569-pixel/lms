package com.example.lms.learn.dto;

public record LearnChatQueryRequest(
        String question,
        String ragContext,
        Long lessonId,
        String lessonTitle,
        Double currentTimeSec,
        Double durationSec,
        Double watchedPercent,
        Boolean lessonCompleted,
        Integer courseCompletedLessons,
        Integer courseTotalLessons,
        Integer coursePercent
) {
}
