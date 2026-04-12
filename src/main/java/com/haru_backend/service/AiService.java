package com.haru_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_backend.domain.Feedback;
import com.haru_backend.domain.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
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
                                     String category, String difficulty, String answerKeywords) {
        String prompt = buildPrompt(questionContent, answerContent, category, difficulty, answerKeywords);

        // 1차 시도
        try {
            return callAiAndParse(answerId, prompt);
        } catch (Exception e) {
            log.warn("AI 피드백 1차 시도 실패, 재시도: answerId={}", answerId, e);
        }

        // 재시도 1회
        try {
            return callAiAndParse(answerId, prompt);
        } catch (Exception e) {
            log.error("AI 피드백 재시도도 실패: answerId={}", answerId, e);
            throw new RuntimeException("AI 피드백 분석에 실패했습니다", e);
        }
    }

    private Feedback callAiAndParse(Long answerId, String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 개발자 면접 답변을 평가하는 전문 면접관입니다. 아래 형식의 JSON으로만 응답하세요. 다른 텍스트는 포함하지 마세요."),
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
    }

    private String buildPrompt(String questionContent, String answerContent,
                                String category, String difficulty, String answerKeywords) {
        return String.format("""
                질문: %s
                카테고리: %s
                난이도: %s
                참고 키워드: %s

                사용자 답변:
                %s

                아래 JSON 형식으로 평가해주세요:
                {"totalScore": 0-100점, "completeness": "내용 완성도 피드백", "structure": "답변 구조 피드백", "expression": "표현/말투 피드백", "specificity": "구체성 피드백", "improvedAnswer": "개선된 모범 답변"}
                """, questionContent, category,
                difficulty != null ? difficulty : "미지정",
                answerKeywords != null ? answerKeywords : "없음",
                answerContent);
    }

    private Feedback parseResponse(Long answerId, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            String json = extractJson(content);
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

    public List<Question> generateQuestions(String category, String difficulty, List<String> techStacks, int count) {
        String stacks = (techStacks != null && !techStacks.isEmpty()) ? String.join(", ", techStacks) : "없음";

        String prompt = String.format("""
                개발자 면접 질문을 %d개 생성해주세요.

                카테고리: %s
                난이도: %s
                관련 기술스택: %s

                아래 JSON 배열 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요:
                [{"content": "질문 내용", "answerKeywords": "핵심 키워드1, 키워드2, 키워드3"}]

                규칙:
                - 실제 면접에서 나올 법한 실무 질문
                - 질문은 구체적이고 명확하게
                - answerKeywords는 채점 참고용 핵심 키워드 (쉼표 구분)
                """, count, category, difficulty, stacks);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 개발자 면접 질문을 생성하는 전문가입니다. 요청된 JSON 배열 형식으로만 응답하세요."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.9
        );

        String responseBody = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseQuestionResponse(responseBody, category, difficulty, techStacks);
    }

    private List<Question> parseQuestionResponse(String responseBody, String category, String difficulty, List<String> techStacks) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            String json = extractJsonArray(content);
            JsonNode questionsNode = objectMapper.readTree(json);

            String stacksJson = objectMapper.writeValueAsString(techStacks != null ? techStacks : List.of());

            List<Question> questions = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                questions.add(Question.builder()
                        .content(node.path("content").asText())
                        .category(category)
                        .difficulty(difficulty)
                        .relatedStacks(stacksJson)
                        .answerKeywords(node.path("answerKeywords").asText())
                        .build());
            }
            return questions;
        } catch (Exception e) {
            log.error("AI 질문 생성 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("AI 질문 생성 응답 파싱에 실패했습니다", e);
        }
    }

    private String extractJsonArray(String content) {
        int start = content.indexOf("[");
        int end = content.lastIndexOf("]");
        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("AI 응답에서 JSON 배열을 찾을 수 없습니다: " + content);
        }
        return content.substring(start, end + 1);
    }

    private String extractJson(String content) {
        // 첫 번째 '{' ~ 마지막 '}' 사이를 JSON으로 추출
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("AI 응답에서 JSON을 찾을 수 없습니다: " + content);
        }
        return content.substring(start, end + 1);
    }
}
