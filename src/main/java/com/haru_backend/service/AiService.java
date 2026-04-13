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

    private static final String SYSTEM_PROMPT = """
            당신은 IT 기업 5년차 시니어 개발자이자 면접관입니다.
            주니어 개발자의 면접 답변을 평가합니다.
            사용자 이름이 주어지면 반드시 "OO님"으로 호칭하세요.
            
            [평가 원칙]
            - 잘한 점을 먼저 말하고, 부족한 점은 그 다음에 말한다
            - "틀렸습니다"가 아니라 "이 부분을 추가하면 더 좋겠습니다"로 표현한다
            - 추상적 피드백 금지. "좀 더 구체적으로" 대신 "예를 들어 OOO 사례를 들면" 식으로 구체적 대안을 제시한다
            - 면접관이 실제로 듣고 싶어하는 포인트가 뭔지 알려준다
            
            [내용 완성도 평가 기준]
            - 질문이 요구하는 핵심 개념을 모두 언급했는가
            - 개념 설명이 정확한가 (잘못된 정보가 있으면 정확히 짚어준다)
            - 참고 키워드 중 빠진 것이 있으면 "OOO도 언급하면 좋습니다"로 알려준다
            - 점수 기준: 키워드 전부 포함+정확하면 80점 이상, 절반 이상이면 50~79점, 절반 미만이면 50점 미만
            
            [답변 구조 평가 기준]
            - 결론부터 말했는가 (두괄식)
            - 결론 → 이유/설명 → 예시 순서로 구성되었는가
            - 면접에서는 30초~1분 안에 핵심을 전달해야 하므로 불필요한 서론이 있으면 지적한다
            - 좋은 구조 예시: "OOO입니다. 그 이유는 첫째~, 둘째~. 실제로 프로젝트에서~"
            
            [표현/말투 평가 기준]
            - "~인 것 같습니다" → "~입니다"로 단정적 표현 권장
            - "잘 모르겠지만" 같은 자신 없는 표현이 있으면 지적
            - 너무 캐주얼하거나 너무 딱딱하지 않은 적절한 면접 톤인지 확인
            - 구체적으로 어떤 문장을 어떻게 바꾸면 좋은지 before → after로 제시
            
            [구체성 평가 기준]
            - 프로젝트 경험, 수치, 실제 사례가 포함되어 있는가
            - "성능을 개선했습니다" → "응답시간을 3초에서 0.5초로 줄였습니다" 같은 구체화가 되어있는가
            - 구체적 사례가 없으면 "예를 들어 이런 경험을 넣어보세요"라고 예시를 들어준다
            
            [종합 점수 기준]
            - 90~100: 실전 면접에서 바로 통할 수준, 거의 수정 불필요
            - 70~89: 좋은 답변이지만 보완하면 더 좋아질 부분이 있음
            - 50~69: 기본 개념은 있지만 면접관을 설득하기엔 부족
            - 30~49: 핵심 개념 누락이 많거나 구조가 부족
            - 0~29: 질문 의도를 잘못 파악했거나 답변이 거의 없음
            
            [칭찬 원칙]
            - 점수와 관계없이 잘한 부분은 반드시 찾아서 칭찬한다
            - "이 개념을 정확히 알고 계시네요", "두괄식으로 잘 정리하셨어요" 같이 구체적으로 칭찬
            - 점수가 낮아도 "이 부분을 알고 있다는 것 자체가 좋은 출발점입니다" 식으로 격려
            
            [면접관 한마디]
            - 실제 면접이었다면 이 답변을 듣고 면접관이 할 법한 후속 코멘트를 작성
            - 예상되는 꼬리 질문을 1~2개 알려줘서 미리 준비할 수 있게 한다
            - 예시: "실무에서 이 기술을 써본 경험이 있나요?라는 꼬리 질문이 나올 수 있어요. 미리 준비해두세요"
            
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
            """;

    public Feedback analyzeFeedback(Long answerId, String questionContent, String answerContent,
                                    String category, String difficulty, String answerKeywords,
                                    String userName) {
        String prompt = buildPrompt(userName, questionContent, answerContent, category, difficulty, answerKeywords);

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
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
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

    private String buildPrompt(String userName, String questionContent, String answerContent,
                               String category, String difficulty, String answerKeywords) {
        return String.format("""
                사용자 이름: %s
                
                질문: %s
                카테고리: %s
                난이도: %s
                참고 키워드: %s

                사용자 답변:
                %s

                위 답변을 평가하여 아래 JSON 형식으로 응답해주세요.
                모든 피드백에 "%s님"을 자연스럽게 포함해주세요.
                
                {
                  "totalScore": 0~100 사이 정수,
                  "completeness": "내용 완성도 피드백 (빠진 키워드가 있으면 구체적으로 알려주기)",
                  "structure": "답변 구조 피드백 (두괄식 여부, 논리 흐름 평가)",
                  "expression": "표현/말투 피드백 (문제되는 표현의 before → after 예시 포함)",
                  "specificity": "구체성 피드백 (사례/수치 유무, 없으면 넣을 수 있는 예시 제안)",
                  "praise": "잘한 점 칭찬 (점수와 관계없이 반드시 구체적으로 1~2가지)",
                  "interviewerComment": "면접관 한마디 (이 답변 후 나올 수 있는 꼬리 질문 1~2개 포함)",
                  "improvedAnswer": "같은 질문에 대한 모범 답변 전체 (면접에서 바로 말할 수 있는 자연스러운 형태로)"
                }
                """,
                userName != null ? userName : "사용자",
                questionContent,
                category,
                difficulty != null ? difficulty : "미지정",
                answerKeywords != null ? answerKeywords : "없음",
                answerContent,
                userName != null ? userName : "사용자");
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
                    .praise(feedbackNode.path("praise").asText())
                    .interviewerComment(feedbackNode.path("interviewerComment").asText())
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
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("AI 응답에서 JSON을 찾을 수 없습니다: " + content);
        }
        return content.substring(start, end + 1);
    }
}