package com.haru_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_backend.domain.Feedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    public Feedback analyzeFeedback(Long answerId, String questionContent, String answerContent,
                                     String category, String answerKeywords) {
        String prompt = buildPrompt(questionContent, answerContent, category, answerKeywords);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "당신은 개발자 면접 답변을 분석하는 전문 면접관입니다. 반드시 지정된 JSON 형식으로만 응답하세요."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7
            );

            String responseBody = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(answerId, responseBody);
        } catch (Exception e) {
            log.error("AI 피드백 분석 실패: answerId={}", answerId, e);
            throw new RuntimeException("AI 피드백 분석에 실패했습니다", e);
        }
    }

    private String buildPrompt(String questionContent, String answerContent,
                                String category, String answerKeywords) {
        return String.format("""
                다음 면접 질문에 대한 답변을 분석해주세요.

                [카테고리] %s
                [질문] %s
                [참고 키워드] %s
                [답변] %s

                아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이):
                {
                    "totalScore": 0~100 사이 정수,
                    "completeness": "내용 완성도에 대한 피드백",
                    "structure": "답변 구조에 대한 피드백",
                    "expression": "표현/말투에 대한 피드백",
                    "specificity": "구체성에 대한 피드백",
                    "improvedAnswer": "개선된 답변 예시"
                }
                """, category, questionContent,
                answerKeywords != null ? answerKeywords : "없음",
                answerContent);
    }

    private Feedback parseResponse(Long answerId, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // JSON 블록만 추출 (```json ... ``` 형태 대응)
            String json = content;
            if (content.contains("```")) {
                int start = content.indexOf("{");
                int end = content.lastIndexOf("}") + 1;
                json = content.substring(start, end);
            }

            JsonNode feedbackNode = objectMapper.readTree(json);

            return Feedback.builder()
                    .answerId(answerId)
                    .totalScore(feedbackNode.path("totalScore").asInt())
                    .completeness(feedbackNode.path("completeness").asText())
                    .structure(feedbackNode.path("structure").asText())
                    .expression(feedbackNode.path("expression").asText())
                    .specificity(feedbackNode.path("specificity").asText())
                    .improvedAnswer(feedbackNode.path("improvedAnswer").asText())
                    .build();
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("AI 응답 파싱에 실패했습니다", e);
        }
    }
}
