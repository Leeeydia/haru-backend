package com.haru_backend.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haru_backend.domain.Question;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDetailResponse {

    private Long id;
    private String content;
    private String category;
    private String difficulty;
    private List<String> relatedStacks;
    private List<String> answerKeywords;
    private LocalDateTime createdAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static QuestionDetailResponse from(Question question) {
        return QuestionDetailResponse.builder()
                .id(question.getId())
                .content(question.getContent())
                .category(question.getCategory())
                .difficulty(question.getDifficulty())
                .relatedStacks(parseJsonArray(question.getRelatedStacks()))
                .answerKeywords(parseJsonArray(question.getAnswerKeywords()))
                .createdAt(question.getCreatedAt())
                .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
