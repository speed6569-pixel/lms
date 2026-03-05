package com.example.lms.learn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
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

    @Value("${app.gateway.config-path:${OPENCLAW_CONFIG_PATH:~/.openclaw/openclaw.json}}")
    private String gatewayConfigPath;

    public String ask(Long loginUserId, Long courseId, String question, String ragContext) {
        try {
            String sessionKey = buildSessionKey(loginUserId, courseId);
            String systemPrompt = "제공된 참고데이터(ragContext)를 우선 근거로 답변하세요.";
            String userPrompt = "질문:\n" + question + "\n\n참고데이터:\n" + (ragContext == null ? "" : ragContext);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-openclaw-agent-id", gatewayAgentId);
            headers.set("x-openclaw-session-key", sessionKey);
            String token = resolveGatewayToken();
            if (token != null && !token.isBlank()) {
                headers.setBearerAuth(token);
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
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            if (code == 401) return "게이트웨이 인증에 실패했습니다. OPENCLAW_GATEWAY_TOKEN을 확인해 주세요.";
            if (code == 404) return "게이트웨이 채팅 엔드포인트를 찾을 수 없습니다.";
            return "게이트웨이 호출 실패 (HTTP " + code + ")";
        } catch (Exception e) {
            return "게이트웨이 오류: " + (e.getMessage() == null ? "unknown" : e.getMessage());
        }
    }

    private String buildSessionKey(Long loginUserId, Long courseId) {
        String user = String.valueOf(loginUserId == null ? 0L : loginUserId);
        String course = String.valueOf(courseId == null ? 0L : courseId);
        return "agent:chatbot:tutor:user:" + user + ":course:" + course;
    }

    private String resolveGatewayToken() {
        if (gatewayToken != null && !gatewayToken.isBlank()) return gatewayToken;
        try {
            String home = System.getProperty("user.home");
            String path = gatewayConfigPath != null && gatewayConfigPath.startsWith("~/")
                    ? home + gatewayConfigPath.substring(1)
                    : gatewayConfigPath;
            String json = Files.readString(Path.of(path));
            String key = "\"token\"";
            int k = json.indexOf(key);
            if (k < 0) return null;
            int colon = json.indexOf(':', k + key.length());
            if (colon < 0) return null;
            int firstQuote = json.indexOf('"', colon + 1);
            if (firstQuote < 0) return null;
            int secondQuote = json.indexOf('"', firstQuote + 1);
            if (secondQuote < 0) return null;
            String token = json.substring(firstQuote + 1, secondQuote).trim();
            return token.isBlank() ? null : token;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String trimTrailingSlash(String s) {
        if (s == null || s.isBlank()) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
