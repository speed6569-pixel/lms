package com.example.lms.learn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class LearnChatService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.gateway.url:${OPENCLAW_GATEWAY_URL:http://127.0.0.1:18789}}")
    private String gatewayUrl;

    @Value("${app.gateway.token:${OPENCLAW_GATEWAY_TOKEN:}}")
    private String gatewayToken;

    @Value("${app.gateway.agent-id:chatbot}")
    private String gatewayAgentId;

    public String ask(Long loginUserId, Long courseId, String question, String ragContext) {
        String sessionKey = buildSessionKey(loginUserId, courseId);
        String systemPrompt = "제공된 참고데이터(ragContext)를 우선 근거로 답변하세요.";
        String userPrompt = "질문:\n" + question + "\n\n참고데이터:\n" + (ragContext == null ? "" : ragContext);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-openclaw-agent-id", gatewayAgentId);
        headers.set("x-openclaw-session-key", sessionKey);
        if (gatewayToken != null && !gatewayToken.isBlank()) {
            headers.setBearerAuth(gatewayToken);
        }

        Map<String, Object> body = Map.of(
                "model", "openclaw:" + gatewayAgentId,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.2
        );

        String url = trimTrailingSlash(gatewayUrl) + "/v1/chat/completions";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map<?, ?> root = response.getBody();
        if (root == null) return "응답 생성 실패";

        Object choicesObj = root.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) return "응답 생성 실패";
        Object firstObj = choices.get(0);
        if (!(firstObj instanceof Map<?, ?> first)) return "응답 생성 실패";
        Object msgObj = first.get("message");
        if (!(msgObj instanceof Map<?, ?> message)) return "응답 생성 실패";
        Object content = message.get("content");
        return content == null ? "응답 생성 실패" : String.valueOf(content);
    }

    private String buildSessionKey(Long loginUserId, Long courseId) {
        String user = String.valueOf(loginUserId == null ? 0L : loginUserId);
        String course = String.valueOf(courseId == null ? 0L : courseId);
        return "agent:chatbot:tutor:user:" + user + ":course:" + course;
    }

    private String trimTrailingSlash(String s) {
        if (s == null || s.isBlank()) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
